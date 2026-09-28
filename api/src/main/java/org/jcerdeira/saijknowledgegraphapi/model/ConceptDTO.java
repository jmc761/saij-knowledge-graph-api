package org.jcerdeira.saijknowledgegraphapi.model;

import java.util.List;

/**
 * Data Transfer Object representing a concept in the knowledge graph.
 * Includes references to broader, narrower, and related concepts with their labels.
 */
public record ConceptDTO(
        String id,
        String uri,
        String label,
        List<String> synonyms,
        ConceptReference broader,
        List<ConceptReference> narrower,
        List<ConceptReference> related,
        String sourceUri
) {}