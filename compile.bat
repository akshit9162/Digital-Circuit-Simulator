@echo off
REM Digital Circuit Simulator v2.0 - Compile and Run Script
REM Works with paths containing spaces

echo.
echo ==========================================
echo Digital Circuit Simulator v2.0
echo ==========================================
echo.

set "SRC_DIR=src"
set "BIN_DIR=bin"

REM Check if Java compiler is installed
javac -version >nul 2>&1
if errorlevel 1 (
    echo Error: Java compiler [javac] not found!
    echo Please install Java Development Kit [JDK] and add it to your PATH.
    echo Download from: https://adoptium.net/
    pause
    exit /b 1
)

REM Create directories
if not exist "%BIN_DIR%" mkdir "%BIN_DIR%"
if not exist "circuits" mkdir "circuits"

echo Compiling all Java source files...
echo.

REM Compile using MainWindow as entry point - javac resolves all dependencies
javac -d "%BIN_DIR%" -sourcepath "%SRC_DIR%" "%SRC_DIR%\com\circuitsimulator\gui\MainWindow.java"

if errorlevel 1 (
    echo.
    echo *** COMPILATION FAILED ***
    pause
    exit /b 1
)

echo.
echo Compilation successful! [All classes compiled]
echo.
echo Starting Digital Circuit Simulator...
echo.

java -cp "%BIN_DIR%" com.circuitsimulator.gui.MainWindow

pause
