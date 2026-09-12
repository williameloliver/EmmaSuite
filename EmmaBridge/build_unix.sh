#!/usr/bin/env bash
set -e
rm -rf build dist
mkdir -p build/classes dist
find src -name "*.java" > sources.txt
javac -encoding UTF-8 -source 1.8 -target 1.8 -d build/classes @sources.txt
printf "Main-Class: com.william.emmabridge.Main\n" > build/manifest.txt
jar cfm dist/EmmaBridge.jar build/manifest.txt -C build/classes .
rm sources.txt
echo "Creado: dist/EmmaBridge.jar"
