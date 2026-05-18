#!/bin/bash
imageTag=$1
registry=$2

if [ -z "$imageTag" ]; then
  echo "Error: imageTag is not set"
  exit 1
fi

if [ -z "$registry" ]; then
  echo "Error: registry is not set"
  exit 1
fi

sed -i "s|image: ai-mcp|image: $imageTag|g" "docker-compose.yml"

cat <<EOF > scripts/after-install.sh 
#!/bin/bash
aws ecr get-login-password --region eu-west-1 \
| docker login --username AWS --password-stdin $registry
 
EOF

echo "Artifacts updated successfully"
