# Rebuild and Deploy Frontend with Fixed API URLs
# This script rebuilds the frontend with corrected environment configuration

Write-Host "🔨 Rebuilding Frontend Docker Image..." -ForegroundColor Cyan

# Navigate to frontend directory
cd "c:\Rakesh New\spring learning\Mentoring Course\Chat Application\chat project\basic-chat-app\frontend"

Write-Host "`n📦 Building Docker image..." -ForegroundColor Yellow
docker build -t rakes9146/chat-frontend:distributed .

if ($LASTEXITCODE -eq 0) {
    Write-Host "✅ Build successful!" -ForegroundColor Green
    
    Write-Host "`n📤 Pushing to Docker Hub..." -ForegroundColor Yellow
    docker push rakes9146/chat-frontend:distributed
    
    if ($LASTEXITCODE -eq 0) {
        Write-Host "✅ Push successful!" -ForegroundColor Green
        
        Write-Host "`n🚀 Ready to deploy to AWS Instance 6" -ForegroundColor Cyan
        Write-Host "`nRun these commands on AWS:" -ForegroundColor White
        Write-Host "  ssh -i `$KEY ec2-user@54.154.129.84" -ForegroundColor Yellow
        Write-Host "  docker-compose down" -ForegroundColor Yellow
        Write-Host "  docker-compose pull" -ForegroundColor Yellow
        Write-Host "  docker-compose up -d" -ForegroundColor Yellow
        Write-Host "  docker-compose logs -f frontend" -ForegroundColor Yellow
    } else {
        Write-Host "❌ Push failed!" -ForegroundColor Red
    }
} else {
    Write-Host "❌ Build failed!" -ForegroundColor Red
}

Write-Host "`n📋 What was fixed:" -ForegroundColor Cyan
Write-Host "  - Angular environment.ts: Changed URLs from localhost to relative paths" -ForegroundColor White
Write-Host "  - Angular environment.prod.ts: Changed URLs from localhost to relative paths" -ForegroundColor White
Write-Host "  - Nginx will now proxy requests to AWS Gateway at 54.217.247.163:8082" -ForegroundColor White
Write-Host "  - WebSocket connections will go through nginx to 35.153.96.103:8083" -ForegroundColor White
