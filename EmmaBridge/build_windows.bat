@echo off
setlocal
if exist build rmdir /s /q build
if exist dist rmdir /s /q dist
mkdir build\classes
mkdir dist
dir /s /b src\*.java > sources.txt
javac -encoding UTF-8 -source 1.8 -target 1.8 -d build\classes @sources.txt
if errorlevel 1 exit /b 1
echo Main-Class: com.william.emmabridge.Main> build\manifest.txt
jar cfm dist\EmmaBridge.jar build\manifest.txt -C build\classes .
del sources.txt
echo Creado: dist\EmmaBridge.jar
