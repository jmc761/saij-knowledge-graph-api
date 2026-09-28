# SAIJ Knowledge Graph API

REST API for querying the SAIJ legal thesaurus.

## Project layout

- `api/`: Java source and Maven project.
- `docker/`: Dockerfile and Compose configuration.
- `postman/`: API collection and local environment.

## Run with Docker

Requires Docker with Compose and Bash. From the repository root:

```sh
./docker.sh up
```

The image builds the application with Maven and Java; neither is required on the host. Once started, the API is available at `http://localhost:8080`:

```sh
curl http://localhost:8080/api/v1/concepts/top-terms
```

On first startup, the API loads the bundled RDF file. Docker stores the graph in a named volume, so it persists when the container is stopped or recreated.

## Docker commands

| Command | Action |
| --- | --- |
| `./docker.sh build` | Build the image without starting the API. |
| `./docker.sh up` | Build and start the API in the background. |
| `./docker.sh logs` | Follow the API logs. |
| `./docker.sh down` | Stop the API and retain the graph data. |
| `./docker.sh destroy` | Stop the API and delete the graph data and local image. |

## Test with Postman

Import the [collection](postman/saij-knowledge-graph-api.postman_collection.json) and [local environment](postman/saij-local.postman_environment.json) into Postman. Select **SAIJ Knowledge Graph API - Local**, then send requests or run the collection. The environment points to `http://localhost:8080` and uses concept ID `779` by default. The collection also works without an environment using its own defaults.

Keep credentials and other secrets out of committed Postman files.
