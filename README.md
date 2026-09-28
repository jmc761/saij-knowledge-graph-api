# SAIJ Knowledge Graph API

REST API for querying the SAIJ legal thesaurus.

## Run with Docker

Requires Docker with Compose. From the project root, run:

```sh
docker compose up --build -d
```

The image builds the application with Maven and Java; neither is required on the host. Once started, the API is available at `http://localhost:8080`:

```sh
curl http://localhost:8080/api/v1/concepts/top-terms
```

On first startup, the API loads the bundled RDF file. Docker stores the graph in a named volume, so it persists when the container is stopped or recreated.

## Test with Postman

Import the [collection](postman/saij-knowledge-graph-api.postman_collection.json) and [local environment](postman/saij-local.postman_environment.json) into Postman. Select **SAIJ Knowledge Graph API - Local**, then send requests or run the collection. The environment points to `http://localhost:8080` and uses concept ID `779` by default. The collection also works without an environment using its own defaults.

Keep credentials and other secrets out of committed Postman files.

## Manage the service

- View logs: `docker compose logs -f api`
- Stop the service: `docker compose down`
- Stop the service and delete the graph data: `docker compose down -v`
