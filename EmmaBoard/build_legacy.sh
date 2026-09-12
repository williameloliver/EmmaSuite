#!/usr/bin/env bash
set -euo pipefail

SDK="${ANDROID_SDK_ROOT:-${ANDROID_HOME:-}}"
if [ -z "$SDK" ]; then
  echo "ERROR: ANDROID_SDK_ROOT/ANDROID_HOME no está definido."
  exit 1
fi

PLATFORM="$SDK/platforms/android-25/android.jar"
BT="$SDK/build-tools/25.0.3"
AAPT="$BT/aapt"
DX="$BT/dx"
ZIPALIGN="$BT/zipalign"

for f in "$PLATFORM" "$AAPT" "$DX" "$ZIPALIGN"; do
  if [ ! -e "$f" ]; then
    echo "ERROR: Falta $f"
    exit 1
  fi
done

cd "$(dirname "$0")"
rm -rf build-legacy
mkdir -p build-legacy/classes build-legacy/out

echo "== Java =="
java -version
javac -version

echo "== Compilando Java para Android =="
find app/src/main/java -name "*.java" | sort > build-legacy/sources.txt

javac \
  -encoding UTF-8 \
  -source 1.6 \
  -target 1.6 \
  -bootclasspath "$PLATFORM" \
  -d build-legacy/classes \
  @build-legacy/sources.txt

echo "== Generando classes.dex =="
"$DX" \
  --dex \
  --output=build-legacy/classes.dex \
  build-legacy/classes

echo "== Empaquetando manifest/resources =="
"$AAPT" package \
  -f \
  -M app/src/main/AndroidManifest.xml \
  -I "$PLATFORM" \
  -F build-legacy/EmmaBoard-unsigned-unaligned.apk

echo "== Agregando classes.dex =="
(
  cd build-legacy
  zip -q -j EmmaBoard-unsigned-unaligned.apk classes.dex
)

echo "== Alineando APK =="
"$ZIPALIGN" -f 4 \
  build-legacy/EmmaBoard-unsigned-unaligned.apk \
  build-legacy/EmmaBoard-aligned-unsigned.apk

echo "== Firmando APK con firma JAR/v1 =="
jarsigner \
  -keystore app/legacy-debug.keystore \
  -storepass emma1234 \
  -keypass emma1234 \
  -sigalg SHA1withRSA \
  -digestalg SHA1 \
  build-legacy/EmmaBoard-aligned-unsigned.apk \
  emma

cp build-legacy/EmmaBoard-aligned-unsigned.apk build-legacy/out/app-debug.apk

echo "== Verificando firma =="
jarsigner -verify build-legacy/out/app-debug.apk

echo
echo "LISTO:"
ls -lh build-legacy/out/app-debug.apk
