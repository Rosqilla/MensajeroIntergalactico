# Script de PowerShell para lanzar múltiples instancias del juego P2P
# Ejecuta 2 instancias con puertos diferentes

Write-Host "=========================================" -ForegroundColor Cyan
Write-Host "   LANZADOR MULTI-INSTANCIA P2P" -ForegroundColor Cyan
Write-Host "=========================================" -ForegroundColor Cyan
Write-Host ""

Write-Host "Compilando el proyecto..." -ForegroundColor Yellow

# Compilar el proyecto
javac -d bin -sourcepath src `
    src\GameMain.java `
    src\controller\*.java `
    src\model\*.java `
    src\model\network\*.java `
    src\model\generators\*.java `
    src\view\*.java

if ($LASTEXITCODE -ne 0) {
    Write-Host "Error en la compilación" -ForegroundColor Red
    Read-Host "Presiona Enter para salir"
    exit 1
}

Write-Host ""
Write-Host "Compilación exitosa!" -ForegroundColor Green
Write-Host ""
Write-Host "Lanzando instancias..." -ForegroundColor Yellow
Write-Host "  - Instancia 1: Puerto 8888" -ForegroundColor White
Write-Host "  - Instancia 2: Puerto 8889" -ForegroundColor White
Write-Host ""

# Lanzar primera instancia
Start-Process -FilePath "java" -ArgumentList "-cp", "bin", "GameMain", "8888" -WindowStyle Normal

# Esperar 2 segundos
Start-Sleep -Seconds 2

# Lanzar segunda instancia
Start-Process -FilePath "java" -ArgumentList "-cp", "bin", "GameMain", "8889" -WindowStyle Normal

Write-Host ""
Write-Host "=========================================" -ForegroundColor Cyan
Write-Host "Instancias lanzadas correctamente!" -ForegroundColor Green
Write-Host "Mueve las naves en cada ventana para" -ForegroundColor White
Write-Host "ver la sincronización P2P en acción." -ForegroundColor White
Write-Host "=========================================" -ForegroundColor Cyan
Write-Host ""
Read-Host "Presiona Enter para cerrar este script"
