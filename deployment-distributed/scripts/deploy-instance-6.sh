#!/bin/bash

# Instance 6 Deployment Script - Frontend (Nginx)
# This script is executed on the Lightsail instance via SSH

set -e  # Exit on any error

REGISTRY="rakes9146"
COMPOSE_FILE="docker-compose.yml"

echo "=========================================="
echo "Deploying Instance 6: Frontend"
echo "=========================================="

# Navigate to deployment directory
cd ~/deployment/instance-6-frontend || exit 1

echo ""
echo "Step 1: Pulling latest Docker image..."
docker pull ${REGISTRY}/chat-frontend:latest

echo ""
echo "Step 2: Stopping current container..."
docker-compose down

echo ""
echo "Step 3: Starting updated container..."
docker-compose up -d

echo ""
echo "Step 4: Waiting for service to initialize (20 seconds)..."
sleep 20

echo ""
echo "Step 5: Checking container status..."
docker-compose ps

echo ""
echo "Step 6: Checking Frontend health..."
if curl -f http://localhost/ > /dev/null 2>&1; then
    echo "✅ Frontend is healthy"
else
    echo "⚠️  Frontend health check pending (may still be starting)"
fi

echo ""
echo "=========================================="
echo "Instance 6 deployment completed!"
echo "=========================================="
echo "Frontend URL: http://$(curl -s ifconfig.me)"
