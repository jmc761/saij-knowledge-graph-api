package org.jcerdeira.saijknowledgegraphapi.repository;

/**
 * Class holding SPARQL query constants.
 */
public final class GraphQueries {

    private GraphQueries() {
        // Prevent instantiation
    }

    public static final String FIND_CONCEPT = """
            PREFIX skos: <http://www.w3.org/2004/02/skos/core#>
            SELECT ?label ?altLabel ?broader ?broaderLabel ?narrower ?narrowerLabel ?related ?relatedLabel ?source WHERE {
              ?targetUri skos:prefLabel ?label .
              OPTIONAL { ?targetUri skos:altLabel ?altLabel }
              OPTIONAL {
                ?targetUri skos:broader ?broader .
                ?broader skos:prefLabel ?broaderLabel .
              }
              OPTIONAL {
                ?targetUri skos:narrower ?narrower .
                ?narrower skos:prefLabel ?narrowerLabel .
              }
              OPTIONAL {
                ?targetUri skos:related ?related .
                ?related skos:prefLabel ?relatedLabel .
              }
              OPTIONAL { ?targetUri skos:exactMatch ?source }
            }
            """;
}