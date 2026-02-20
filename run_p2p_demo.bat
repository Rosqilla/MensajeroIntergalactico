@echo off
REM Script para lanzar múltiples instancias del juego P2P
REM Ejecuta 2 instancias con puertos diferentes

echo =========================================
echo   LANZADOR MULTI-INSTANCIA P2P
echo =========================================
echo.
echo Compilando el proyecto...
javac -d bin -sourcepath src src\GameMain.java src\controller\*.java src\model\*.java src\model\network\*.java src\model\generators\*.java src\view\*.java

if errorlevel 1 (
    echo Error en la compilación
    pause
    exit /b 1
)

echo.
echo Compilación exitosa!
echo.
echo Lanzando instancias...
echo   - Instancia 1: Puerto 8888
echo   - Instancia 2: Puerto 8889
echo.
echo Las ventanas aparecerán en breve...
echo.

REM Lanzar primera instancia
start "Jugador 1 [8888]" java -cp bin GameMain 8888

REM Esperar 2 segundos antes de lanzar la segunda
timeout /t 2 /nobreak > nul

REM Lanzar segunda instancia
start "Jugador 2 [8889]" java -cp bin GameMain 8889

echo.
echo =========================================
echo Instancias lanzadas correctamente!
echo Mueve las naves en cada ventana para
echo ver la sincronización P2P en acción.
echo =========================================
pause
