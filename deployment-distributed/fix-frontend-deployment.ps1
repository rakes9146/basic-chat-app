# Quick fix for frontend deployment error
# This script uploads the corrected files to AWS Instance 6

Write-Host "🔧 Fixing Frontend Deployment..." -ForegroundColor Cyan

# Set your SSH key path (UPDATE THIS!)
$KEY = "C:\path\to\your-key.pem"
$FRONTEND_IP = "54.154.129.84"

# Upload corrected docker-compose file
Write-Host "📤 Uploading corrected docker-compose.yml..." -ForegroundColor Yellow
scp -i $KEY "instance-6-frontend\docker-compose.yml" "ec2-user@${FRONTEND_IP}:~/docker-compose.yml"

# Upload AWS-specific nginx configuration
Write-Host "📤 Uploading nginx-aws.conf..." -ForegroundColor Yellow
scp -i $KEY "instance-6-frontend\nginx-aws.conf" "ec2-user@${FRONTEND_IP}:~/nginx-aws.conf"

Write-Host "`n✅ Files uploaded!" -ForegroundColor Green
Write-Host "`nNow run these commands to deploy:" -ForegroundColor Cyan
Write-Host "  ssh -i $KEY ec2-user@$FRONTEND_IP" -ForegroundColor White
Write-Host "  docker-compose down" -ForegroundColor White
Write-Host "  docker-compose pull" -ForegroundColor White
Write-Host "  docker-compose up -d" -ForegroundColor White
Write-Host "  docker-compose logs -f frontend" -ForegroundColor White
Write-Host "`nThe nginx configuration error should be resolved!" -ForegroundColor Green
