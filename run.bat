@echo off
setlocal
cd /d "%~dp0"

if not defined JAVAFX_HOME set "JAVAFX_HOME=%~dp0javafx-sdk-25"

set "JAVAFX_LIB=%JAVAFX_HOME%\lib"
set "OUT=out"

if not exist "%JAVAFX_LIB%\javafx.controls.jar" (
    echo JavaFX SDK not found under "%JAVAFX_HOME%".
    echo Set JAVAFX_HOME to the SDK root containing lib\javafx.controls.jar,
    echo or place the SDK in a javafx-sdk-25 folder next to this script.
    exit /b 1
)

if not exist "%OUT%" mkdir "%OUT%"

echo Compiling...
dir /s /b src\*.java > sources.txt
javac --module-path "%JAVAFX_LIB%" --add-modules javafx.controls,javafx.media -d "%OUT%" @sources.txt
if errorlevel 1 (
    echo.
    echo COMPILATION FAILED
    del sources.txt
    exit /b 1
)
del sources.txt

if not exist "%OUT%\com\swift\resources" mkdir "%OUT%\com\swift\resources"
xcopy /y /e /i /q src\com\swift\resources "%OUT%\com\swift\resources" >nul

echo Starting Swift...
java "-Djava.library.path=%JAVAFX_HOME%\bin" --module-path "%JAVAFX_LIB%;%OUT%" --add-modules javafx.controls,javafx.media -m swift/com.swift.SwiftApp
endlocal
