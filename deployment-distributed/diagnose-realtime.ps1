# Quick Diagnostic Script for Real-Time Features
# Run this to check WebSocket, Redis, and Kafka connectivity

param(
    [Parameter(Mandatory=$true)]
    [string]$KeyPath
)

$INSTANCE2 = "35.153.96.103"
$INSTANCE4 = "3.147.109.193"
$INSTANCE5 = "98.89.238.241"
$INSTANCE6 = "54.154.129.84"

Write-Host "`n🔍 Checking Real-Time Features Configuration..." -ForegroundColor Cyan

# Check Instance 2 - Message Service
Write-Host "`n📡 Instance 2 - Message Service" -ForegroundColor Yellow
Write-Host "Checking Redis connection..." -ForegroundColor White
ssh -i $KeyPath ec2-user@$INSTANCE2 "docker exec chat-message-service redis-cli -h $INSTANCE5 ping 2>&1"

Write-Host "`nChecking Kafka connection..." -ForegroundColor White
ssh -i $KeyPath ec2-user@$INSTANCE2 "docker exec chat-message-service bash -c 'timeout 2 nc -zv $INSTANCE4 9092 2>&1'"

Write-Host "`nChecking WebSocket endpoint..." -ForegroundColor White
ssh -i $KeyPath ec2-user@$INSTANCE2 "curl -I http://localhost:8083/ws 2>&1 | head -5"

Write-Host "`nMessage Service Logs (last 10 lines):" -ForegroundColor White
ssh -i $KeyPath ec2-user@$INSTANCE2 "docker-compose logs --tail=10 message-service 2>&1"

# Check Instance 4 - Kafka
Write-Host "`n📨 Instance 4 - Kafka" -ForegroundColor Yellow
Write-Host "Checking Kafka topics..." -ForegroundColor White
ssh -i $KeyPath ec2-user@$INSTANCE4 "docker exec kafka kafka-topics --list --bootstrap-server localhost:9092 2>&1"

# Check Instance 5 - Redis
Write-Host "`n💾 Instance 5 - Redis" -ForegroundColor Yellow
Write-Host "Checking Redis status..." -ForegroundColor White
ssh -i $KeyPath ec2-user@$INSTANCE5 "docker exec redis redis-cli ping 2>&1"

Write-Host "`nChecking online users in Redis..." -ForegroundColor White
ssh -i $KeyPath ec2-user@$INSTANCE5 "docker exec redis redis-cli KEYS 'user:*:online' 2>&1"

# Check Instance 6 - Frontend nginx
Write-Host "`n🌐 Instance 6 - Frontend" -ForegroundColor Yellow
Write-Host "Checking nginx WebSocket proxy config..." -ForegroundColor White
ssh -i $KeyPath ec2-user@$INSTANCE6 "docker exec chat-frontend grep -A 5 'location /ws' /etc/nginx/conf.d/default.conf 2>&1"

Write-Host "`n📋 Diagnosis Complete!" -ForegroundColor Green
Write-Host "`n⚠️  Common Issues:" -ForegroundColor Yellow
Write-Host "  1. If Redis ping fails → Check Instance 5 firewall allows port 6379 from $INSTANCE2" -ForegroundColor White
Write-Host "  2. If Kafka connection fails → Check Instance 4 firewall allows port 9092 from $INSTANCE2" -ForegroundColor White
Write-Host "  3. If WebSocket returns 404 → Check message-service is running properly" -ForegroundColor White
Write-Host "  4. If nginx config missing → Re-upload nginx-aws.conf and restart frontend" -ForegroundColor White

Write-Host "`n🔧 To fix, add this firewall rule to Instance 2:" -ForegroundColor Cyan
Write-Host "   Port: 8083, Source: 54.154.129.84/32 (Frontend WebSocket)" -ForegroundColor White
