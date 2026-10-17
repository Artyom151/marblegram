#!/bin/sh
# Lightweight headless Android emulator for Marblegram tests (~1.5 GB RAM).
# Usage: emu.sh start|stop|install|shot <file.png>|tap x y|text "..."|log
E=$HOME/Android/Sdk/emulator
export ANDROID_SDK_ROOT=$HOME/.cache/marblegram-buildenv/sdk ANDROID_HOME=$ANDROID_SDK_ROOT
ADB="adb -s emulator-5554"
APK=$(dirname "$0")/../../TMessagesProj_App/build/outputs/apk/afat/debug/app.apk
case "$1" in
start)
	LD_LIBRARY_PATH=$E/lib64:$E/lib64/qt/lib nohup fglibc run $E/emulator \
		-avd marble -no-window -no-audio -no-boot-anim -no-snapshot-save \
		-gpu swiftshader_indirect -memory 1536 -cores 2 \
		> /tmp/marble-emu.log 2>&1 &
	echo "log: /tmp/marble-emu.log"
	adb wait-for-device; until [ "$($ADB shell getprop sys.boot_completed 2>/dev/null | tr -d '\r')" = 1 ]; do sleep 2; done
	$ADB shell settings put global window_animation_scale 1; echo booted ;;
stop) $ADB emu kill ;;
install) $ADB install -r -g "$APK" ;;
run) $ADB shell monkey -p org.telegram.messenger.beta -c android.intent.category.LAUNCHER 1 ;;
shot) $ADB exec-out screencap -p > "${2:-/tmp/marble-shot.png}" ;;
tap) $ADB shell input tap "$2" "$3" ;;
swipe) $ADB shell input swipe "$2" "$3" "$4" "$5" "${6:-300}" ;;
text) $ADB shell input text "$(echo "$2" | sed 's/ /%s/g')" ;;
key) $ADB shell input keyevent "$2" ;;
view) command -v scrcpy >/dev/null || { echo "install: doas apk add scrcpy"; exit 1; }
	SCRCPY_SERVER_PATH=$HOME/.cache/scrcpy/scrcpy-server nohup scrcpy -s emulator-5554 --window-title Marblegram --no-audio --max-fps 60 -m 1024 >/tmp/marble-scrcpy.log 2>&1 &
	echo "window opened" ;;
ui) $ADB exec-out uiautomator dump /dev/tty ;;
log) $ADB logcat -d -b main -b crash | grep -E "Marble|AndroidRuntime|FATAL" | tail -${2:-80} ;;
*) echo "view|start|stop|install|run|shot|tap|swipe|text|key|ui|log" ;;
esac
