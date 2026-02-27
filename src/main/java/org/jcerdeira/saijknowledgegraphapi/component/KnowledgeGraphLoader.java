package org.jcerdeira.saijknowledgegraphapi.component;

import org.apache.jena.rdf.model.*;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.SKOS;
import org.jcerdeira.saijknowledgegraphapi.repository.KnowledgeGraphRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Component responsible for seeding and refactoring the Knowledge Graph data on startup.
 */
@Component
public class KnowledgeGraphLoader {

    private final KnowledgeGraphRepository repository;
    private final ResourceLoader resourceLoader;
    private final String myDomain;
    private final String saijBase;
    private final String initialFile;

    public KnowledgeGraphLoader(KnowledgeGraphRepository repository,
                                ResourceLoader resourceLoader,
                                @Value("${app.graph.domain}") String myDomain,
                                @Value("${app.graph.saij-base}") String saijBase,
                                @Value("${app.graph.initial-file}") String initialFile) {
        this.repository = repository;
        this.resourceLoader = resourceLoader;
        this.myDomain = myDomain;
        this.saijBase = saijBase;
        this.initialFile = initialFile;
    }

    /**
     * Initializes the Knowledge Graph on application startup.
     * <p>
     * Checks if the TDB2 dataset is empty. If so, it loads the initial RDF/XML file,
     * parses it, and triggers the graph refactoring process within a write transaction.
     * </p>
     */
    @PostConstruct
    public void init() {
        repository.getDataset().executeWrite(() -> {
            Model model = repository.getDataset().getDefaultModel();
            if (model.isEmpty()) {
                org.springframework.core.io.Resource springResource =
                        resourceLoader.getResource(initialFile);

                try (InputStream inputStream = springResource.getInputStream()) {
                    RDFDataMgr.read(model, inputStream, Lang.RDFXML);
                    refactorGraph(model);
                    System.out.println(">>> Knowledge Graph initialized and refactored.");
                } catch (IOException e) {
                    throw new RuntimeException("Could not load initial RDF/XML file", e);
                }
            }
        });
    }

    /**
     * Refactors the raw SAIJ graph into the internal domain model.
     * <p>
     * Iterates over all SKOS Concepts, creates new resources using the application's domain,
     * links them to the original resources via 'exactMatch', and transforms relationships
     * to point to the new domain URIs.
     * </p>
     *
     * @param model The Jena Model containing the RDF data.
     */
    private void refactorGraph(Model model) {
        Property exactMatch = model.createProperty(SKOS.uri + "exactMatch");
        List<Statement> newStatements = new ArrayList<>();

        ResIterator it = model.listSubjectsWithProperty(RDF.type, SKOS.Concept);

        while (it.hasNext()) {
            Resource saijResource = it.next();
            String originalUri = saijResource.getURI();

            if (originalUri != null && originalUri.contains("skosTema=")) {
                String id = extractIdFromUri(originalUri);
                if (id != null) {
                    Resource myResource = model.createResource(myDomain + id);

                    newStatements.add(model.createStatement(myResource, RDF.type, SKOS.Concept));
                    newStatements.add(model.createStatement(myResource, exactMatch, saijResource));

                    saijResource.listProperties().forEachRemaining(stmt -> {
                        Property p = stmt.getPredicate();
                        RDFNode o = stmt.getObject();

                        if (o.isResource()) {
                            String objectUri = o.asResource().getURI();
                            if (objectUri != null && objectUri.startsWith(saijBase)) {
                                String relId = extractIdFromUri(objectUri);
                                if (relId != null) {
                                    o = model.createResource(myDomain + relId);
                                }
                            }
                        }

                        if (!p.equals(RDF.type)) {
                            newStatements.add(model.createStatement(myResource, p, o));
                        }
                    });
                }
            }
        }
        model.add(newStatements);
    }

    /**
     * Extracts the unique identifier from a given URI string.
     * <p>
     * Attempts to parse the URI using UriComponentsBuilder to retrieve the 'skosTema' query parameter.
     * Falls back to simple string manipulation if parsing fails.
     * </p>
     *
     * @param uri The URI string to parse.
     * @return The extracted ID, or null if extraction fails.
     */
    private String extractIdFromUri(String uri) {
        try {
            return UriComponentsBuilder.fromUriString(uri)
                    .build()
                    .getQueryParams()
                    .getFirst("skosTema");
        } catch (Exception e) {
            if (uri.contains("skosTema=")) {
                return uri.substring(uri.indexOf("skosTema=") + 9);
            }
            return null;
        }
    }
}