package org.jcerdeira.saijknowledgegraphapi.service;

import org.jcerdeira.saijknowledgegraphapi.model.ConceptDTO;
import org.jcerdeira.saijknowledgegraphapi.model.ConceptReference;
import org.jcerdeira.saijknowledgegraphapi.repository.KnowledgeGraphRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

/**
 * Service responsible for business logic related to the Knowledge Graph.
 * <p>
 * This service delegates data access and graph operations to the {@link KnowledgeGraphRepository}.
 * </p>
 */
@Service
public class KnowledgeGraphService {

    private final KnowledgeGraphRepository repository;

    public KnowledgeGraphService(KnowledgeGraphRepository repository) {
        this.repository = repository;
    }

    /**
     * Retrieves a concept by its unique identifier.
     *
     * @param id The unique identifier of the concept.
     * @return An Optional containing the ConceptDTO if found, or empty otherwise.
     */
    public Optional<ConceptDTO> getConceptById(String id) {
        return repository.getConceptById(id);
    }

    /**
     * Retrieves the top-level terms (concepts with no broader concept).
     *
     * @return A list of ConceptReference objects representing the top terms.
     */
    public List<ConceptReference> fetchTopTerms() {
        return repository.fetchTopTerms();
    }
}