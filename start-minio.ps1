Write-Host "Levantando MinIO..." -ForegroundColor Cyan

docker compose -f .\docker\docker-compose.yml up -d

Write-Host ""
Write-Host "MinIO iniciado correctamente." -ForegroundColor Green
Write-Host "MinIO Console: http://localhost:9001"
Write-Host "Usuario: admin"
Write-Host "Password: admin123"
Write-Host ""
Write-Host "Ahora abrí dos terminales en VS Code:" -ForegroundColor Yellow
Write-Host ""
Write-Host "Terminal 1 - Backend:"
Write-Host "cd backend"
Write-Host "mvn spring-boot:run"
Write-Host ""
Write-Host "Terminal 2 - Frontend:"
Write-Host "cd frontend"
Write-Host "ng serve"
Write-Host ""
Write-Host "Frontend: http://localhost:4200"
Write-Host "Backend:  http://localhost:8080"