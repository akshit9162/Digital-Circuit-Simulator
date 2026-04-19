#!/bin/bash
# Digital Circuit Simulator v2.0 - Build and Run Script (Linux/Mac)

echo ""
echo "=========================================="
echo "Digital Circuit Simulator v2.0 - Build"
echo "=========================================="
echo ""

SRC_DIR="src"
BIN_DIR="bin"

if ! command -v javac &> /dev/null; then
    echo "Error: Java compiler (javac) not found!"
    echo "Install JDK: sudo apt install default-jdk (Ubuntu) or brew install openjdk (Mac)"
    exit 1
fi

mkdir -p "$BIN_DIR"
mkdir -p "circuits"

echo "[1/2] Compiling source files..."
javac -d "$BIN_DIR" -sourcepath "$SRC_DIR" "$SRC_DIR/com/circuitsimulator/gui/MainWindow.java"

if [ $? -eq 0 ]; then
    echo "[2/2] Build successful!"
    echo ""
    echo "Starting application..."
    java -cp "$BIN_DIR" com.circuitsimulator.gui.MainWindow
else
    echo "*** COMPILATION FAILED ***"
    exit 1
fi
