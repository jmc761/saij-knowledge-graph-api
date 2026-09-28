package org.jcerdeira.saijknowledgegraphapi.controller;

import org.jcerdeira.saijknowledgegraphapi.model.ConceptDTO;
import org.jcerdeira.saijknowledgegraphapi.model.ConceptReference;
import org.jcerdeira.saijknowledgegraphapi.service.KnowledgeGraphService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * REST Controller for exposing Knowledge Graph operations.
 * <p>
 * This controller provides endpoints to interact with the legal knowledge graph,
 * allowing clients to retrieve concept details by their unique identifier.
 * </p>
 */
@RestController
@RequestMapping("/api/v1/concepts")
public class KnowledgeGraphController {

    private final KnowledgeGraphService knowledgeGraphService;

    /**
     * Constructs the KnowledgeGraphController with the required service.
     *
     * @param knowledgeGraphService The service handling knowledge graph logic.
     */
    public KnowledgeGraphController(KnowledgeGraphService knowledgeGraphService) {
        this.knowledgeGraphService = knowledgeGraphService;
    }

    /**
     * Retrieves a specific concept by its ID.
     * <p>
     * This endpoint accepts a concept ID, queries the knowledge graph service,
     * and returns the concept details if found. If the concept does not exist,
     * it returns a 404 Not Found response.
     * </p>
     *
     * @param id The unique identifier of the concept to retrieve.
     * @return A ResponseEntity containing the ConceptDTO if found (200 OK), or 404 Not Found.
     */
    @GetMapping("/{id}")
    public ResponseEntity<ConceptDTO> getConcept(@PathVariable String id) {
        return knowledgeGraphService.getConceptById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Retrieves the top-level terms (concepts with no broader concept).
     *
     * @return A ResponseEntity containing a list of ConceptReference objects representing the top terms.
     */
    @GetMapping("/top-terms")
    public ResponseEntity<List<ConceptReference>> getTopTerms() {
        List<ConceptReference> topTerms = knowledgeGraphService.fetchTopTerms();
        return ResponseEntity.ok(topTerms);
    }
}