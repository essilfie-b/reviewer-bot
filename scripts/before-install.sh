#!/bin/bash
cd /home/projects/amali-ai-mcp
docker-compose down || true
docker rm -f ai-mcp || true
