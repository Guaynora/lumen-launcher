#!/usr/bin/env bash
# Compila Lumen sin Gradle: aapt2 + javac + d8 + apksigner
set -euo pipefail
cd "$(dirname "$0")"
B=~/.local/android-build; BT=$B/sdk/build-tools/34.0.0; AJ=$B/sdk/platforms/android-34/android.jar
export JAVA_HOME=$B/jdk PATH=$B/jdk/bin:$PATH
rm -rf build && mkdir -p build/gen build/classes build/dex
$BT/aapt2 compile --dir res -o build/res.zip
$BT/aapt2 link -I $AJ --manifest AndroidManifest.xml --java build/gen -o build/unsigned.apk build/res.zip
javac -nowarn -Xlint:-options --release 11 -classpath $AJ -d build/classes $(find src build/gen -name '*.java') 2>&1 | grep -v "^Note:" || true
[ -n "$(find build/classes -name '*.class')" ] || { echo "javac falló"; exit 1; }
$BT/d8 --lib $AJ --min-api 26 --release --output build/dex $(find build/classes -name '*.class')
python3 -I -c "import zipfile,sys; z=zipfile.ZipFile('build/unsigned.apk','a'); z.write('build/dex/classes.dex','classes.dex'); z.close()"
$BT/zipalign -f 4 build/unsigned.apk build/aligned.apk
KS=$B/lumen.keystore
[ -f $KS ] || keytool -genkeypair -keystore $KS -storepass lumenpass -keypass lumenpass -alias lumen -keyalg RSA -keysize 2048 -validity 10000 -dname "CN=Lumen" >/dev/null 2>&1
$BT/apksigner sign --ks $KS --ks-pass pass:lumenpass --out lumen.apk build/aligned.apk
ls -la lumen.apk
