#!/usr/bin/env bash
# Builds bt-pairing-fix.apk without Gradle: aapt2 -> javac -> d8 -> zipalign -> apksigner.
#
# Needs a JDK (11+) and the Android SDK with "platforms;android-29" and any build-tools.
# Environment (all optional):
#   ANDROID_HOME / ANDROID_SDK_ROOT  SDK location
#   KEYSTORE, KS_PASS, KS_ALIAS      signing key; a local debug.keystore is created if absent
set -euo pipefail
cd "$(dirname "$0")"

SDK=${ANDROID_HOME:-${ANDROID_SDK_ROOT:-/opt/homebrew/share/android-commandlinetools}}
JAR="$SDK/platforms/android-29/android.jar"
BT=$(ls -d "$SDK"/build-tools/* 2>/dev/null | sort -V | tail -1)
[[ -f "$JAR" ]] || { echo "android.jar not found: install 'platforms;android-29' into $SDK" >&2; exit 1; }
[[ -n "$BT" ]] || { echo "build-tools not found in $SDK" >&2; exit 1; }
if [[ -z "${JAVA_HOME:-}" && -d /opt/homebrew/opt/openjdk/libexec/openjdk.jdk ]]; then
    export JAVA_HOME=/opt/homebrew/opt/openjdk/libexec/openjdk.jdk/Contents/Home
fi
[[ -n "${JAVA_HOME:-}" ]] && export PATH="$JAVA_HOME/bin:$PATH"

KEYSTORE=${KEYSTORE:-debug.keystore}
KS_PASS=${KS_PASS:-android}
KS_ALIAS=${KS_ALIAS:-pairok}
OUT=bt-pairing-fix.apk

rm -rf build && mkdir -p build/res build/gen build/classes
"$BT/aapt2" compile --dir res -o build/res/res.zip
"$BT/aapt2" link -I "$JAR" --manifest AndroidManifest.xml --java build/gen \
    -o build/unsigned.apk build/res/res.zip
javac --release 11 -nowarn -classpath "$JAR" -d build/classes \
    $(find src build/gen -name '*.java')
"$BT/d8" --release --min-api 24 --lib "$JAR" --output build $(find build/classes -name '*.class')
(cd build && zip -q unsigned.apk classes.dex)
"$BT/zipalign" -f 4 build/unsigned.apk build/aligned.apk

# An update installs over an existing copy only when signed with the same key.
if [[ ! -f "$KEYSTORE" ]]; then
    keytool -genkeypair -keystore "$KEYSTORE" -storepass "$KS_PASS" -keypass "$KS_PASS" \
        -alias "$KS_ALIAS" -keyalg RSA -keysize 2048 -validity 10000 \
        -dname "CN=xiaomi-tv-bt-pairing-fix" >/dev/null 2>&1
    echo "created signing key $KEYSTORE"
fi
"$BT/apksigner" sign --ks "$KEYSTORE" --ks-pass "pass:$KS_PASS" --key-pass "pass:$KS_PASS" \
    --ks-key-alias "$KS_ALIAS" --out "$OUT" build/aligned.apk 2>/dev/null
echo "built $(pwd)/$OUT"
