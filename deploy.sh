#!/usr/bin/env bash
#
# One-shot: build (if needed), install to an Android TV (real device or the
# running emulator), grant the overlay permission, and launch the clock.
#
# Usage:
#   ./deploy.sh                 # emulator, or a TV already connected over adb
#   ./deploy.sh 192.168.1.50    # connect to a real TV over the network first
#
set -euo pipefail

PKG="com.tatav.tvclock"
ADB="$HOME/Library/Android/sdk/platform-tools/adb"
APK="app/build/outputs/apk/debug/app-debug.apk"
export JAVA_HOME=/opt/homebrew/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home

cd "$(dirname "$0")"

# 0. Connect over the network if an IP was passed.
if [[ $# -ge 1 ]]; then
  echo "==> Connecting to $1:5555 ..."
  "$ADB" connect "$1:5555"
fi

echo "==> Devices:"
"$ADB" devices -l

# 1. Build the APK if it isn't there.
if [[ ! -f "$APK" ]]; then
  echo "==> Building APK ..."
  ./gradlew assembleDebug --no-daemon
fi

# 2. Install (reinstall over any existing copy).
echo "==> Installing $APK ..."
"$ADB" install -r "$APK"

# 3. Grant the "draw over other apps" permission (the key step on Android TV).
echo "==> Granting overlay permission ..."
"$ADB" shell appops set "$PKG" SYSTEM_ALERT_WINDOW allow

# 4. Launch the app (which starts the overlay service).
echo "==> Launching app ..."
"$ADB" shell am start -n "$PKG/.MainActivity"

echo "==> Done. The clock should now be in the top-right corner."
