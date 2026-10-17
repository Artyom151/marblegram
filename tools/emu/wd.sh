#!/bin/sh
# Waydroid (LXC Android, native Mesa/Wayland) for Marblegram tests.
# One-time: ./wd.sh setup   (needs doas)
# Daily:    ./wd.sh start ; ./wd.sh install ; ./wd.sh run
PKG=org.telegram.messenger.beta
APK=$(dirname "$0")/../../TMessagesProj_App/build/outputs/apk/afat/debug/app.apk
SU=$(command -v doas || echo sudo)

ip_of() { waydroid status 2>/dev/null | awk '/IP address/ && $3 != "UNKNOWN" {print $3}'; }
ADB() { adb -s "$(ip_of):5555" "$@"; }

case "$1" in
setup)
	$SU apk add waydroid waydroid-openrc lxc dnsmasq iptables
	$SU rc-update add waydroid-container default
	$SU rc-service waydroid-container start
	# LineageOS 20 (Android 13) vanilla image, ~900 MB
	$SU waydroid init -s VANILLA
	# Lower RAM / disable heavy stuff
	$SU sh -c 'cat >> /var/lib/waydroid/waydroid_base.prop <<EOF
dalvik.vm.heapsize=256m
ro.config.low_ram=true
persist.waydroid.multi_windows=false
EOF'
	echo "setup done; now: ./wd.sh start" ;;
start)
	say() { printf '\033[1;36m[%s]\033[0m %s\n' "$(date +%T)" "$*"; }
	say "1/5 reset old session + container service"
	waydroid session stop >/dev/null 2>&1
	# OpenRC enables cpuset in cgroup v2 root -> Android can't mount v1 cpuset -> init SIGKILLs hwcomposer
	$SU rc-service waydroid-container restart
	# OpenRC (re)enables cpuset in the cgroup v2 root on service start, so release it AFTER the restart
	$SU sh -c 'echo -cpuset > /sys/fs/cgroup/cgroup.subtree_control'
	if grep -qw cpuset /sys/fs/cgroup/cgroup.subtree_control; then say "   WARN: cpuset still bound to cgroup v2"; else say "   cpuset released from cgroup v2"; fi
	say "2/5 kernel/android log (dmesg -w) -> /tmp/marble-boot.log"
	$SU sh -c 'dmesg -C; dmesg -w' 2>/dev/null | awk '!/binder|RLIMIT/ {print; fflush()}' > /tmp/marble-boot.log &
	DM=$!
	say "3/5 waydroid show-full-ui (starts session + gives hwcomposer a Wayland surface)"
	nohup waydroid show-full-ui >/tmp/marble-wd.log 2>&1 &
	( $SU sh -c 'for i in $(seq 60); do [ "$(lxc-info -P /var/lib/waydroid/lxc -n waydroid -sH)" = RUNNING ] && break; sleep 0.5; done; sleep 1; umask 022; rm -f /tmp/marble-logcat.log; lxc-attach -P /var/lib/waydroid/lxc -n waydroid --clear-env -- /system/bin/logcat -b all -v threadtime > /tmp/marble-logcat.log 2>&1; chmod 644 /tmp/marble-logcat.log' ) &
	LC=$!
	say "4/5 waiting for Android boot (IP)"
	i=0; seen=0; last=""
	until [ -n "$(ip_of)" ]; do
		st=$(waydroid status 2>/dev/null | awk -F'\t' '/Session|Container/ {sub(":","",$1); printf "%s=%s ", $1, $2}')
		[ "$st" != "$last" ] && { say "   $st"; last=$st; }
		n=$(wc -l < /tmp/marble-boot.log)
		[ "$n" -gt "$seen" ] && { tail -n +$((seen+1)) /tmp/marble-boot.log | grep -E 'starting service|Service .*(exited|signal|crash|killed)|reboot|critical|FATAL|Abort|failed' | sed 's/^\[[ 0-9.]*\] /   · /' | cut -c1-140; seen=$n; }
		i=$((i+1))
		if [ $i -gt 90 ] || waydroid status | grep -q 'Container:.*STOPPED' && [ $i -gt 10 ]; then
			say "FAILED: Android died. Last log lines:"; grep -iE 'exited|signal|fatal|abort|fail|reboot|critical' /tmp/marble-boot.log | tail -15; tail -5 /tmp/marble-wd.log
			kill $DM $LC 2>/dev/null; $SU chmod 644 /tmp/marble-logcat.log /tmp/marble-boot.log; grep -E '^[EF]/' /tmp/marble-logcat.log | grep -iE 'surfaceflinger|hwcomposer|gralloc|egl|mesa|DEBUG' | tail -15; exit 1
		fi
		sleep 2
	done
	say "5/5 IP $(ip_of) - waiting for adb + boot_completed (up to 4 min)"
	T="$(ip_of):5555"; i=0
	until adb connect "$T" 2>/dev/null | grep -q 'connected to'; do
		i=$((i+1)); [ $i -gt 48 ] && { say "FAILED: adb never came up"; kill $DM 2>/dev/null; exit 1; }
		waydroid status | grep -q 'Container:.*STOPPED' && { say "FAILED: container stopped"; kill $DM 2>/dev/null; exit 1; }
		n=$(wc -l < /tmp/marble-boot.log)
		[ "$n" -gt "$seen" ] && { tail -n +$((seen+1)) /tmp/marble-boot.log | grep -E "starting service '(zygote|surfaceflinger|adbd)'|Service .*(signal|crash)" | sed 's/^\[[ 0-9.]*\] /   · /' | cut -c1-140; seen=$n; }
		sleep 5
	done
	until [ "$(adb -s "$T" shell getprop sys.boot_completed 2>/dev/null | tr -d '\r')" = 1 ]; do sleep 3; done
	kill $DM 2>/dev/null
	say "ready: $(ip_of):5555" ;;
stop) waydroid session stop ;;
debug)
	waydroid session stop 2>/dev/null; $SU rc-service waydroid-container restart
	nohup waydroid session start >/tmp/marble-wd.log 2>&1 &
	sleep 30
	{ echo "== status"; waydroid status
	  echo "== lxc"; $SU lxc-info -P /var/lib/waydroid/lxc -n waydroid
	  echo "== android ps"; $SU waydroid shell -- ps -A 2>&1 | head -40
	  echo "== logcat"; $SU waydroid logcat -d 2>&1 | grep -E ' [EF] ' | tail -40
	  echo "== dmesg"; $SU dmesg | grep -E "init|ueventd|servicemanager|apexd|zygote|audit" | grep -vE "binder:|RLIMIT" | tail -200
	  echo "== waydroid.log"; tail -40 /var/lib/waydroid/waydroid.log
	} > /tmp/marble-wd-debug.txt 2>&1
	echo "saved /tmp/marble-wd-debug.txt" ;;
fix)
	# Android init needs writable cgroups (else hwcomposer is SIGKILLed and the container dies)
	$SU sed -i -E 's/cgroup:(ro|rw)( |$)/cgroup:rw:force\2/' /usr/lib/waydroid/data/configs/config_base /var/lib/waydroid/lxc/waydroid/config
	grep -h 'mount.auto' /var/lib/waydroid/lxc/waydroid/config
	# LXC re-binds cpuset to cgroup v2 -> no /dev/cpuset in Android -> init SIGKILLs services with task_profiles.
	# Override hwcomposer rc without task_profiles via Waydroid overlay.
	$SU sh -c 'd=/var/lib/waydroid/overlay/vendor/etc/init; mkdir -p $d; cat > $d/android.hardware.graphics.composer@2.1-service.rc <<RC
service vendor.hwcomposer-2-1 /vendor/bin/hw/android.hardware.graphics.composer@2.1-service
    interface android.hardware.graphics.composer@2.1::IComposer default
    class hal animation
    user system
    group graphics drmrpc
    capabilities SYS_NICE
    onrestart restart surfaceflinger
RC'
	echo "hwcomposer rc overridden" 
	waydroid session stop 2>/dev/null; $SU rc-service waydroid-container restart
	echo "now: ./wd.sh start" ;;
soft)
	# software rendering fallback; props must live in waydroid.cfg [properties] (upgrade regenerates base.prop)
	$SU sh -c 'c=/var/lib/waydroid/waydroid.cfg
		sed -i "/^ro.hardware.gralloc *=/d;/^ro.hardware.egl *=/d;/^dalvik.vm.heapsize *=/d;/^ro.config.low_ram *=/d;s/^auto_adb = .*/auto_adb = True/" $c
		sed -i "/^\[properties\]/a ro.hardware.gralloc = default\nro.hardware.egl = swiftshader\ndalvik.vm.heapsize = 256m\nro.config.low_ram = true" $c
		waydroid upgrade -o'
	grep -E "gralloc|egl" /var/lib/waydroid/waydroid_base.prop
	echo "now: ./wd.sh start" ;;
nopsi)
	# Host kernel has PSI disabled -> Android lmkd exits -> boot loop.
	# Replace lmkd with a sleeper via Waydroid overlay (no reboot needed).
	$SU sh -c 'd=/var/lib/waydroid/overlay/system/bin; mkdir -p $d
		printf "#!/system/bin/sh\nwhile true; do sleep 86400; done\n" > $d/lmkd; chmod 755 $d/lmkd'
	echo "lmkd stubbed; now: ./wd.sh start" ;;
install) waydroid app install "$APK" ;;
run) waydroid app launch $PKG ;;
shot) ADB exec-out screencap -p > "${2:-/tmp/marble-shot.png}" ;;
tap) ADB shell input tap "$2" "$3" ;;
swipe) ADB shell input swipe "$2" "$3" "$4" "$5" "${6:-300}" ;;
text) ADB shell input text "$(echo "$2" | sed 's/ /%s/g')" ;;
key) ADB shell input keyevent "$2" ;;
ui) ADB exec-out uiautomator dump /dev/tty ;;
log) ADB logcat -d -b main -b crash | grep -E "Marble|AndroidRuntime|FATAL" | tail -${2:-80} ;;
adb) shift; ADB "$@" ;;
*) echo "setup|start|stop|install|run|shot|tap|swipe|text|key|ui|log|adb" ;;
esac
