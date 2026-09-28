#!/usr/bin/env bash
set -euo pipefail

script_dir="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
compose=(docker compose -f "$script_dir/docker/compose.yaml")

usage() {
    cat <<'USAGE'
Usage: ./docker.sh {build|up|logs|down|destroy}

  build    Build the API image
  up       Build and start the API
  logs     Follow API logs
  down     Stop the API and keep graph data
  destroy  Stop the API and delete graph data and the local image
USAGE
}

if (( $# != 1 )); then
    usage >&2
    exit 2
fi

case "$1" in
    build)   exec "${compose[@]}" build ;;
    up)      exec "${compose[@]}" up --build -d ;;
    logs)    exec "${compose[@]}" logs -f api ;;
    down)    exec "${compose[@]}" down ;;
    destroy) exec "${compose[@]}" down --volumes --rmi local ;;
    *)       usage >&2; exit 2 ;;
esac
