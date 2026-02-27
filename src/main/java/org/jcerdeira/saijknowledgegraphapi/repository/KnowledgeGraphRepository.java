package org.jcerdeira.saijknowledgegraphapi.repository;

import lombok.Getter;
import org.apache.jena.query.*;
import org.apache.jena.tdb2.TDB2Factory;
import org.jcerdeira.saijknowledgegraphapi.model.ConceptDTO;
import org.jcerdeira.saijknowledgegraphapi.model.ConceptReference;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Repository responsible for managing the Knowledge Graph using Apache Jena TDB2.
 */
@Repository
public class KnowledgeGraphRepository {

    @Getter
    private final Dataset dataset;

    private final String myDomain;

    /**
     * Constructs the KnowledgeGraphRepository with necessary dependencies and configuration.
     *
     * @param myDomain       The base URI for the application's internal concept representation.
     * @param dataPath       The file system path where the TDB2 database is stored.
     */
    public KnowledgeGraphRepository(@Value("${app.graph.domain}") String myDomain,
                                    @Value("${app.graph.data-path}") String dataPath) {
        this.myDomain = myDomain;
        this.dataset = TDB2Factory.connectDataset(dataPath);
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