#!/bin/bash

# Instance 1 Deployment Script - Eureka Server + API Gateway
# This script is executed on the Lightsail instance via SSH

set -e  # Exit on any error

REGISTRY="rakes9146"
COMPOSE_FILE="docker-compose.yml"

echo "=========================================="
echo "Deploying Instance 1: Eureka + Gateway"
echo "=========================================="

# Navigate to deployment directory
cd ~/deployment/instance-1-eureka-gateway || exit 1

echo ""
echo "Step 1: Pulling latest Docker images..."
docker pull ${REGISTRY}/chat-eureka:latest
docker pull ${REGISTRY}/chat-api-gateway:latest

echo ""
echo "Step 2: Stopping current containers..."
docker-compose down

echo ""
echo "Step 3: Starting updated containers..."
docker-compose up -d

echo ""
echo "Step 4: Waiting for services to initialize (30 seconds)..."
sleep 30

echo ""
echo "Step 5: Checking container status..."
docker-compose ps

echo ""
echo "Step 6: Checking Eureka Server health..."
if curl -f http://localhost:8761/ > /dev/null 2>&1; then
    echo "✅ Eureka Server is healthy"
else
    echo "⚠️  Eureka Server health check pending (may still be starting)"
fi

echo ""
echo "Step 7: Checking API Gateway health..."
if curl -f http://localhost:8082/actuator/health > /dev/null 2>&1; then
    echo "✅ API Gateway is healthy"
else
    echo "⚠️  API Gateway health check pending (may still be starting)"
fi

echo ""
echo "=========================================="
echo "Instance 1 deployment completed!"
echo "=========================================="
echo "Eureka Dashboard: http://$(curl -s ifconfig.me):8761"
echo "API Gateway: http://$(curl -s ifconfig.me):8082"
