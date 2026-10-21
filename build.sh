#!/data/data/com.termux/files/usr/bin/bash
set -e
TMP=build_tmp
OUT=slime.apk
rm -rf $TMP $OUT
mkdir -p $TMP
ANDROID_JAR="$HOME/android-sdk/platforms/android-34/android.jar"
echo "[*] Compiling..."
javac -source 8 -target 8 -bootclasspath $ANDROID_JAR -d $TMP $(find app/src/main/java -name "*.java")

echo "[*] DEXing with low RAM..."
export _JAVA_OPTIONS="-Xmx512m"
D8=$(find $HOME/android-sdk/build-tools -name d8 -type f | sort -V | tail -1)
# Use --no-desugar? No, need desugar but small heap
$D8 --lib $ANDROID_JAR --output $TMP --min-api 21 $TMP/com/slime/*.class 2>&1 || $D8 -JXmx512M --lib $ANDROID_JAR --output $TMP $TMP/com/slime/*.class

DEX=$(find $TMP -name "classes.dex" | head -1)
echo "[*] Dex $DEX"

echo "[*] Packaging..."
aapt package -f -M app/src/main/AndroidManifest.xml -S app/src/main/res -I /system/framework/framework-res.apk -F $TMP/unaligned.apk
cd $TMP
aapt add unaligned.apk classes.dex >/dev/null 2>&1 || aapt add -f unaligned.apk classes.dex
cd ..
cp $TMP/unaligned.apk $OUT
[ -f debug.keystore ] || keytool -genkey -v -keystore debug.keystore -storepass android -alias androiddebugkey -keypass android -keyalg RSA -keysize 2048 -validity 10000 -dname "CN=Debug" >/dev/null 2>&1
apksigner sign --ks debug.keystore --ks-pass pass:android $OUT 2>/dev/null || jarsigner -keystore debug.keystore -storepass android $OUT androiddebugkey >/dev/null 2>&1 || true
echo "[*] Done $OUT $(du -h $OUT | cut -f1)"
