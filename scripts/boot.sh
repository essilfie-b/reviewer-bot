#!/bin/bash
# ==============================================================================
# AWS CodeDeploy ApplicationStart Hook Script
# Description: Fetches securely stored environment variables from AWS SSM 
#              Parameter Store, generates the .env file dynamically, and starts 
#              the Docker containers.
# ==============================================================================

# 1. Determine Environment (Dev vs Prod) based on the CodeDeploy Deployment Group
# CodeDeploy automatically injects the DEPLOYMENT_GROUP_NAME variable during execution.
if [[ "$DEPLOYMENT_GROUP_NAME" == *"-prod"* ]]; then
    ENV="prod"
else
    ENV="dev"
fi

# 2. Determine APP_DIR and SERVICE_NAME based on DEPLOYMENT_GROUP_NAME
APP_DIR="/home/projects/amali-ai-mcp"
SERVICE_NAME="mcp"


echo "Deployment Group: $DEPLOYMENT_GROUP_NAME"
echo "Resolved APP_DIR: $APP_DIR"
echo "Resolved SERVICE_NAME: $SERVICE_NAME"
echo "Resolved Environment: $ENV"

cd $APP_DIR || exit 1

echo "Fetching $ENV environment variables for $SERVICE_NAME from AWS SSM Parameter Store..."

# 3. Fetch parameters, parse them into KEY=VALUE format, replace __EMPTY__ placeholders, and write to .env
aws ssm get-parameters-by-path \
    --path "/amali-ai/$ENV/$SERVICE_NAME/" \
    --with-decryption \
    --query "Parameters[*].[Name,Value]" \
    --output text | \
    sed "s|^/amali-ai/$ENV/$SERVICE_NAME/||g" | \
    sed 's/\t/=/g' | \
    sed 's/="__EMPTY__"/=""/g' > .env

# Verify if variables were written
if [ -s .env ]; then
    echo "Environment variables successfully written to .env!"
else
    echo "Warning: No environment variables found or written to .env. (This might be normal for some apps)."
fi

# 4. Start the Application
echo "Starting Docker Compose..."

# Determine which docker-compose file to use
COMPOSE_FILE="docker-compose.yml"


docker compose -f $COMPOSE_FILE up -d --build
