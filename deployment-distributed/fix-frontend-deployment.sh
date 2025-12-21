#!/bin/bash
# Quick fix for frontend deployment error
# This script uploads the corrected docker-compose.yml to AWS Instance 6

echo "🔧 Fixing Frontend Deployment..."

# Set your SSH key path
KEY="C:/path/to/your-key.pem"
FRONTEND_IP="54.154.129.84"

# Upload corrected docker-compose file
echo "📤 Uploading corrected docker-compose.yml..."
scp -i $KEY ../instance-6-frontend/docker-compose.yml ec2-user@$FRONTEND_IP:~/docker-compose.yml

echo "✅ File uploaded!"
echo ""
echo "Now SSH to the instance and run:"
echo "  ssh -i $KEY ec2-user@$FRONTEND_IP"
echo "  docker-compose pull"
echo "  docker-compose up -d"
echo ""
echo "The volume mount errors should be resolved!"
