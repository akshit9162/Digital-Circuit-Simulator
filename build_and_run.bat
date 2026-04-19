@echo off
REM Digital Circuit Simulator v2.0 - Build and Run Script

echo.
echo ==========================================
echo Digital Circuit Simulator v2.0 - Build
echo ==========================================
echo.

set "SRC_DIR=src"
set "BIN_DIR=bin"

REM Check if Java compiler is installed
javac -version >nul 2>&1
if errorlevel 1 (
    echo Error: Java compiler [javac] not found!
    echo Please install Java Development Kit [JDK] 8 or higher.
    echo.
    echo Download options:
    echo   - Eclipse Temurin: https://adoptium.net/
    echo   - Oracle JDK: https://www.oracle.com/java/technologies/downloads/
    echo.
    echo After installation, add the JDK bin directory to your system PATH.
    pause
    exit /b 1
)

REM Create directories
if not exist "%BIN_DIR%" mkdir "%BIN_DIR%"
if not exist "circuits" mkdir "circuits"

echo [1/2] Compiling source files...

REM Compile using entry point - javac auto-resolves all dependencies
javac -d "%BIN_DIR%" -sourcepath "%SRC_DIR%" "%SRC_DIR%\com\circuitsimulator\gui\MainWindow.java"

if errorlevel 1 (
    echo.
    echo *** COMPILATION FAILED ***
    echo Please check the error messages above.
    pause
    exit /b 1
)

echo [2/2] Build successful!
echo.
echo Starting application...
echo.

REM Run the application
java -cp "%BIN_DIR%" com.circuitsimulator.gui.MainWindow

pause
