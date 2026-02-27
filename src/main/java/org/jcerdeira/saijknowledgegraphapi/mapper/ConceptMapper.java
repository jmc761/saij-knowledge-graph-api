package org.jcerdeira.saijknowledgegraphapi.mapper;

import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.jcerdeira.saijknowledgegraphapi.model.ConceptDTO;
import org.jcerdeira.saijknowledgegraphapi.model.ConceptReference;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Component responsible for mapping Jena SPARQL ResultSets to Domain DTOs.
 */
@Component
public class ConceptMapper {

    /**
     * Maps a SPARQL ResultSet to a ConceptDTO.
     * <p>
     * Iterates through the ResultSet to aggregate multiple rows (caused by one-to-many relationships
     * like synonyms or narrower concepts) into a single DTO object.
     * </p>
     *
     * @param rs        The Jena ResultSet obtained from the SPARQL query.
     * @param id        The concept ID (used for DTO construction).
     * @param targetUri The full URI of the concept (used for DTO construction).
     * @return An Optional containing the populated ConceptDTO, or empty if the ResultSet is empty.
     */
    public Optional<ConceptDTO> mapToConceptDTO(ResultSet rs, String id, String targetUri) {
        if (!rs.hasNext()) return Optional.empty();

        String label = "";
        ConceptReference broader = null;
        String source = null;
        List<String> synonyms = new ArrayList<>();
        List<ConceptReference> narrower = new ArrayList<>();
        List<ConceptReference> related = new ArrayList<>();

        while (rs.hasNext()) {
            QuerySolution soln = rs.nextSolution();
            
            // We assume the prefLabel is present in all rows for the concept
            if (soln.contains("label")) {
                label = soln.getLiteral("label").getString();
            }

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