#!/bin/sh
# Marblegram release script.
#
#   ./release.sh 1.2.0 [--push] [--build]
#
# 1. Makes sure BuildVars.java contains only the public upstream stubs: if real APP_ID / APP_HASH
#    literals are found they are moved to secrets.properties (gitignored) and replaced by stubs.
# 2. Refuses to continue if secrets.properties is tracked by git or a secret leaked into the index.
# 3. Optionally builds the release APK locally (--build; credentials come from secrets.properties).
# 4. Commits the stub check (if anything changed), tags v<version> and pushes when --push is given
#    (to the "marble" remote if it exists, never to the upstream DrKLO repo).
set -eu

cd "$(dirname "$0")"
VERSION=${1:-}
[ -n "$VERSION" ] || { echo "usage: ./release.sh <version> [--push] [--build]"; exit 1; }
shift
PUSH=0; BUILD=0
for a in "$@"; do
	case "$a" in
		--push) PUSH=1 ;;
		--build) BUILD=1 ;;
		*) echo "unknown option $a"; exit 1 ;;
	esac
done

BV=TMessagesProj/src/main/java/org/telegram/messenger/BuildVars.java
STUB_ID=4
STUB_HASH=014b35b6184100b085b0d0572f9b5103
say() { printf '\033[1;35m[release]\033[0m %s\n' "$*"; }

# --- 1. stub BuildVars ----------------------------------------------------------------------------
REAL_ID=$(sed -n 's/.*public static int APP_ID = \([0-9][0-9]*\);.*/\1/p' "$BV")
REAL_HASH=$(sed -n 's/.*public static String APP_HASH = "\([0-9a-f]\{32\}\)";.*/\1/p' "$BV")
if [ -n "$REAL_ID$REAL_HASH" ] && [ "$REAL_ID" != "$STUB_ID" -o "$REAL_HASH" != "$STUB_HASH" ]; then
	say "real credentials found in BuildVars.java → moving them to secrets.properties"
	{
		[ -n "$REAL_ID" ] && [ "$REAL_ID" != "$STUB_ID" ] && echo "MARBLE_APP_ID=$REAL_ID"
		[ -n "$REAL_HASH" ] && [ "$REAL_HASH" != "$STUB_HASH" ] && echo "MARBLE_APP_HASH=$REAL_HASH"
	} >> secrets.properties
	chmod 600 secrets.properties
	sed -i \
		-e "s/public static int APP_ID = [0-9][0-9]*;/public static int APP_ID = BuildConfig.MARBLE_APP_ID != 0 ? BuildConfig.MARBLE_APP_ID : $STUB_ID;/" \
		-e "s/public static String APP_HASH = \"[0-9a-f]\{32\}\";/public static String APP_HASH = !BuildConfig.MARBLE_APP_HASH.isEmpty() ? BuildConfig.MARBLE_APP_HASH : \"$STUB_HASH\";/" \
		"$BV"
fi
grep -q 'BuildConfig.MARBLE_APP_ID' "$BV" || { say "BuildVars.java is not in the expected format, aborting"; exit 1; }
say "BuildVars.java: stubs ✓"

# --- 2. leak checks -------------------------------------------------------------------------------
if git ls-files --error-unmatch secrets.properties >/dev/null 2>&1; then
	say "secrets.properties is tracked by git! run: git rm --cached secrets.properties"; exit 1
fi
if [ -f secrets.properties ]; then
	for v in $(sed -n 's/^MARBLE_APP_HASH=//p' secrets.properties); do
		if git grep -q "$v" -- . ':!secrets.properties' 2>/dev/null; then
			say "the real APP_HASH is present in tracked files, aborting"; exit 1
		fi
	done
fi
say "no secrets in tracked files ✓"

# --- 3. optional local build ----------------------------------------------------------------------
if [ $BUILD = 1 ]; then
	say "building release APK"
	./gradlew :TMessagesProj_App:assembleAfatRelease --max-workers=2
	ls -la TMessagesProj_App/build/outputs/apk/afat/release/ 2>/dev/null || true
fi

# --- 4. commit, tag, push ---------------------------------------------------------------------------
if ! git diff --quiet -- "$BV" .gitignore; then
	git add "$BV" .gitignore
	git commit -m "release: stub Telegram API credentials for v$VERSION"
fi
git tag -a "v$VERSION" -m "Marblegram v$VERSION"
say "tagged v$VERSION"

if [ $PUSH = 1 ]; then
	REMOTE=marble
	if ! git remote | grep -qx "$REMOTE"; then
		say "no 'marble' remote. Add your fork first: git remote add marble git@github.com:<you>/marblegram.git"; exit 1
	fi
	git push "$REMOTE" HEAD "v$VERSION"
	say "pushed to $REMOTE (GitHub Actions will build the APK)"
fi
