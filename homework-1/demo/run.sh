#!/bin/bash
set -e
cd "$(dirname "$0")/.."

echo "Starting Banking Transactions API..."
echo "Server will be available at http://localhost:8080"
echo "Press Ctrl+C to stop."
echo ""

mvn spring-boot:run
