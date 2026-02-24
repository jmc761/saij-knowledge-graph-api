package org.jcerdeira.saijknowledgegraphapi.model;

import java.util.List;

public record ConceptDTO(
        String id,
        String uri,
        String label,
        List<String> synonyms,
        String broaderUri,      // Higher level term (Parent)
        List<String> narrower,  // Specific terms (Children)
        List<String> related,   // Associated terms (Horizontal)
        String sourceUri
) {}