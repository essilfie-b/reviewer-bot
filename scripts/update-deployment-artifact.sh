#!/bin/bash
imageTag=$1

if [ -z "$imageTag" ]; then
  echo "Error: imageTag is not set"
  exit 1
fi

sed -i "s|image: ai-mcp|image: $imageTag|g" "docker-compose.yml"

cat <<EOF > scripts/after-install.sh 
#!/bin/bash
aws ecr get-login-password --region 'eu-west-1' | docker login --username AWS --password-stdin $imageTag
 
EOF

echo "Artifacts updated successfully"
