#!/bin/bash
# Script para lanzar múltiples instancias del juego P2P
# Compatible con Linux y macOS

echo "========================================="
echo "   LANZADOR MULTI-INSTANCIA P2P"
echo "========================================="
echo ""
echo "Compilando el proyecto..."

# Compilar el proyecto
javac -d bin -sourcepath src \
    src/GameMain.java \
    src/controller/*.java \
    src/model/*.java \
    src/model/network/*.java \
    src/model/generators/*.java \
    src/view/*.java

if [ $? -ne 0 ]; then
    echo "Error en la compilación"
    read -p "Presiona Enter para salir"
    exit 1
fi

echo ""
echo "Compilación exitosa!"
echo ""
echo "Lanzando instancias..."
echo "  - Instancia 1: Puerto 8888"
echo "  - Instancia 2: Puerto 8889"
echo ""

# Lanzar primera instancia en background
java -cp bin GameMain 8888 > /dev/null 2>&1 &
PID1=$!
echo "Instancia 1 iniciada (PID: $PID1)"

# Esperar 2 segundos
sleep 2

# Lanzar segunda instancia en background
java -cp bin GameMain 8889 > /dev/null 2>&1 &
PID2=$!
echo "Instancia 2 iniciada (PID: $PID2)"

echo ""
echo "========================================="
echo "Instancias lanzadas correctamente!"
echo "Mueve las naves en cada ventana para"
echo "ver la sincronización P2P en acción."
echo ""
echo "Para detener:"
echo "  kill $PID1 $PID2"
echo "========================================="
echo ""

# Esperar a que el usuario presione Ctrl+C
trap "kill $PID1 $PID2 2>/dev/null; echo 'Instancias detenidas'; exit" INT TERM

echo "Presiona Ctrl+C para detener todas las instancias..."
wait
