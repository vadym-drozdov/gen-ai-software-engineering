#!/bin/bash
set -e
cd "$(dirname "$0")/.."

echo "Building Docker image..."
docker build -t banking-api .

echo ""
echo "Starting Banking Transactions API in Docker..."
echo "Server will be available at http://localhost:8080"
echo "Press Ctrl+C to stop."
echo ""

docker run --rm -p 8080:8080 banking-api
