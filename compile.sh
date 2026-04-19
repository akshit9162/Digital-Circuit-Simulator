#!/bin/bash
# Digital Circuit Simulator v2.0 - Compile and Run Script (Linux/Mac)

echo ""
echo "=========================================="
echo "Digital Circuit Simulator v2.0"
echo "=========================================="
echo ""

SRC_DIR="src"
BIN_DIR="bin"

# Check if Java compiler is installed
if ! command -v javac &> /dev/null; then
    echo "Error: Java compiler (javac) not found!"
    echo "Please install Java Development Kit (JDK) 8 or higher."
    echo ""
    echo "  Ubuntu/Debian: sudo apt install default-jdk"
    echo "  Mac: brew install openjdk"
    echo "  Or download from: https://adoptium.net/"
    exit 1
fi

# Create directories
mkdir -p "$BIN_DIR"
mkdir -p "circuits"

echo "Compiling all Java source files..."
javac -d "$BIN_DIR" -sourcepath "$SRC_DIR" "$SRC_DIR/com/circuitsimulator/gui/MainWindow.java"

if [ $? -eq 0 ]; then
    echo ""
    echo "Compilation successful!"
    echo ""
    echo "Starting Digital Circuit Simulator..."
    echo ""
    java -cp "$BIN_DIR" com.circuitsimulator.gui.MainWindow
else
    echo ""
    echo "*** COMPILATION FAILED ***"
    exit 1
fi
