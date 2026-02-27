package org.jcerdeira.saijknowledgegraphapi.repository;

import lombok.Getter;
import org.apache.jena.query.*;
import org.apache.jena.tdb2.TDB2Factory;
import org.jcerdeira.saijknowledgegraphapi.model.ConceptDTO;
import org.jcerdeira.saijknowledgegraphapi.mapper.ConceptMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository responsible for managing the Knowledge Graph using Apache Jena TDB2.
 */
@Repository
public class KnowledgeGraphRepository {

    @Getter
    private final Dataset dataset;

    private final String myDomain;
    private final ConceptMapper mapper;

    /**
     * Constructs the KnowledgeGraphRepository with necessary dependencies and configuration.
     *
     * @param myDomain       The base URI for the application's internal concept representation.
     * @param dataPath       The file system path where the TDB2 database is stored.
     * @param mapper         The mapper to convert SPARQL results to DTOs.
     */
    public KnowledgeGraphRepository(@Value("${app.graph.domain}") String myDomain,
                                    @Value("${app.graph.data-path}") String dataPath,
                                    ConceptMapper mapper) {
        this.myDomain = myDomain;
        this.mapper = mapper;
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
        pss.setCommandText(GraphQueries.FIND_CONCEPT);
        pss.setIri("targetUri", targetUri);

        return dataset.calculateRead(() -> {
            try (QueryExecution qexec = QueryExecutionFactory.create(pss.asQuery(), dataset)) {
                return mapper.mapToConceptDTO(qexec.execSelect(), id, targetUri);
            }
        });
    }
}