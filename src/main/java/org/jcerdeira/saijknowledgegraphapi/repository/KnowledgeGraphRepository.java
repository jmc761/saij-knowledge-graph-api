package org.jcerdeira.saijknowledgegraphapi.repository;

import org.apache.jena.query.*;
import org.apache.jena.rdf.model.*;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.tdb2.TDB2Factory;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.SKOS;
import org.jcerdeira.saijknowledgegraphapi.model.ConceptDTO;
import org.jcerdeira.saijknowledgegraphapi.model.ConceptReference;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Repository;
import org.springframework.web.util.UriComponentsBuilder;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Repository responsible for managing the Knowledge Graph using Apache Jena TDB2.
 * <p>
 * This repository handles the initialization of the dataset, loading of initial RDF data,
 * refactoring of the graph structure, and executing SPARQL queries.
 * </p>
 */
@Repository
public class KnowledgeGraphRepository {

    private final ResourceLoader resourceLoader;
    private final Dataset dataset;

    private final String myDomain;
    private final String saijBase;
    private final String initialFile;

    /**
     * Constructs the KnowledgeGraphRepository with necessary dependencies and configuration.
     *
     * @param resourceLoader Spring ResourceLoader to load files from the classpath.
     * @param myDomain       The base URI for the application's internal concept representation.
     * @param saijBase       The base URI of the original SAIJ vocabulary.
     * @param dataPath       The file system path where the TDB2 database is stored.
     * @param initialFile    The path to the initial RDF/XML file to load if the database is empty.
     */
    public KnowledgeGraphRepository(ResourceLoader resourceLoader,
                                    @Value("${app.graph.domain}") String myDomain,
                                    @Value("${app.graph.saij-base}") String saijBase,
                                    @Value("${app.graph.data-path}") String dataPath,
                                    @Value("${app.graph.initial-file}") String initialFile) {
        this.resourceLoader = resourceLoader;
        this.myDomain = myDomain;
        this.saijBase = saijBase;
        this.initialFile = initialFile;
        this.dataset = TDB2Factory.connectDataset(dataPath);
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
        dataset.executeWrite(() -> {
            Model model = dataset.getDefaultModel();
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
            org.apache.jena.rdf.model.Resource saijResource = it.next();
            String originalUri = saijResource.getURI();

            if (originalUri != null && originalUri.contains("skosTema=")) {
                String id = extractIdFromUri(originalUri);
                if (id != null) {
                    org.apache.jena.rdf.model.Resource myResource = model.createResource(myDomain + id);

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

    /**
     * Retrieves a concept by its unique identifier.
     * <p>
     * Constructs a parameterized SPARQL query to fetch the concept's details (labels, relations, source)
     * and executes it within a read transaction.
     * </p>
     *
     * @param id The unique identifier of the concept.
     * @return An Optional containing the ConceptDTO if found, or empty otherwise.
     */
    public Optional<ConceptDTO> getConceptById(String id) {
        String targetUri = myDomain + id;

        ParameterizedSparqlString pss = new ParameterizedSparqlString();
        pss.setCommandText(
                "PREFIX skos: <http://www.w3.org/2004/02/skos/core#> " +
                        "SELECT ?label ?altLabel ?broader ?broaderLabel ?narrower ?narrowerLabel ?related ?relatedLabel ?source WHERE { " +
                        "  ?targetUri skos:prefLabel ?label . " +
                        "  OPTIONAL { ?targetUri skos:altLabel ?altLabel } " +
                        "  OPTIONAL { " +
                        "    ?targetUri skos:broader ?broader . " +
                        "    ?broader skos:prefLabel ?broaderLabel . " +
                        "  } " +
                        "  OPTIONAL { " +
                        "    ?targetUri skos:narrower ?narrower . " +
                        "    ?narrower skos:prefLabel ?narrowerLabel . " +
                        "  } " +
                        "  OPTIONAL { " +
                        "    ?targetUri skos:related ?related . " +
                        "    ?related skos:prefLabel ?relatedLabel . " +
                        "  } " +
                        "  OPTIONAL { ?targetUri skos:exactMatch ?source } " +
                        "}");
        
        pss.setIri("targetUri", targetUri);

        return dataset.calculateRead(() -> executeConceptQuery(pss.asQuery(), id, targetUri));
    }

    /**
     * Executes the SPARQL query and maps the result set to a ConceptDTO.
     * <p>
     * Iterates through the ResultSet to aggregate multiple rows (caused by one-to-many relationships
     * like synonyms or narrower concepts) into a single DTO object.
     * </p>
     *
     * @param query     The SPARQL query to execute.
     * @param id        The concept ID (used for DTO construction).
     * @param targetUri The full URI of the concept (used for DTO construction).
     * @return An Optional containing the populated ConceptDTO, or empty if no results found.
     */
    private Optional<ConceptDTO> executeConceptQuery(Query query, String id, String targetUri) {
        try (QueryExecution qexec = QueryExecutionFactory.create(query, dataset)) {
            ResultSet rs = qexec.execSelect();
            if (!rs.hasNext()) return Optional.empty();

            String label = "";
            ConceptReference broader = null;
            String source = null;
            List<String> synonyms = new ArrayList<>();
            List<ConceptReference> narrower = new ArrayList<>();
            List<ConceptReference> related = new ArrayList<>();

            while (rs.hasNext()) {
                QuerySolution soln = rs.nextSolution();
                label = soln.getLiteral("label").getString();

                if (soln.contains("altLabel")) {
                    String val = soln.getLiteral("altLabel").getString();
                    if (!synonyms.contains(val)) synonyms.add(val);
                }
                if (soln.contains("narrower")) {
                    String uri = soln.getResource("narrower").getURI();
                    String lbl = soln.getLiteral("narrowerLabel").getString();
                    ConceptReference ref = new ConceptReference(uri, lbl);
                    if (!narrower.contains(ref)) narrower.add(ref);
                }
                if (soln.contains("related")) {
                    String uri = soln.getResource("related").getURI();
                    String lbl = soln.getLiteral("relatedLabel").getString();
                    ConceptReference ref = new ConceptReference(uri, lbl);
                    if (!related.contains(ref)) related.add(ref);
                }
                if (soln.contains("broader")) {
                    String uri = soln.getResource("broader").getURI();
                    String lbl = soln.getLiteral("broaderLabel").getString();
                    broader = new ConceptReference(uri, lbl);
                }
                if (soln.contains("source")) {
                    source = soln.getResource("source").getURI();
                }
            }
            return Optional.of(new ConceptDTO(id, targetUri, label, synonyms, broader, narrower, related, source));
        }
    }
}