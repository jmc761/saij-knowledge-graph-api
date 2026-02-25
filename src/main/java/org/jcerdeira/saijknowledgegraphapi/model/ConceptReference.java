package org.jcerdeira.saijknowledgegraphapi.model;

/**
 * Represents a reference to another concept, containing its URI and preferred label.
 * Used for broader, narrower, and related links.
 */
public record ConceptReference(String uri, String label) {
}