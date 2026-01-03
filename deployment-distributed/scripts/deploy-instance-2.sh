#!/bin/bash

# Instance 2 Deployment Script - User Service + Message Service
# This script is executed on the Lightsail instance via SSH

set -e  # Exit on any error

REGISTRY="rakes9146"
COMPOSE_FILE="docker-compose.yml"

echo "=========================================="
echo "Deploying Instance 2: User + Message Services"
echo "=========================================="

# Navigate to deployment directory
cd ~/deployment/instance-2-user-message-services || exit 1

echo ""
echo "Step 1: Pulling latest Docker images..."
docker pull ${REGISTRY}/chat-user-service:latest
docker pull ${REGISTRY}/chat-message-service:latest

echo ""
echo "Step 2: Stopping current containers..."
docker-compose down

echo ""
echo "Step 3: Starting updated containers..."
docker-compose up -d

echo ""
echo "Step 4: Waiting for services to initialize (40 seconds)..."
sleep 40

echo ""
echo "Step 5: Checking container status..."
docker-compose ps

echo ""
echo "Step 6: Checking User Service health..."
if curl -f http://localhost:8081/actuator/health > /dev/null 2>&1; then
    echo "✅ User Service is healthy"
else
    echo "⚠️  User Service health check pending (may still be starting)"
fi

echo ""
echo "Step 7: Checking Message Service health..."
if curl -f http://localhost:8083/actuator/health > /dev/null 2>&1; then
    echo "✅ Message Service is healthy"
else
    echo "⚠️  Message Service health check pending (may still be starting)"
fi

echo ""
echo "=========================================="
echo "Instance 2 deployment completed!"
echo "=========================================="
echo "User Service: http://$(curl -s ifconfig.me):8081"
echo "Message Service: http://$(curl -s ifconfig.me):8083"
