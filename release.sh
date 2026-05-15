#!/usr/bin/env bash
set -euo pipefail

PROJECT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
KEYSTORE="${PROJECT_DIR}/tiaosheng.jks"
PROPS="${PROJECT_DIR}/keystore.properties"
OUT_DIR="${PROJECT_DIR}/release"
RELEASE_APK_SRC="${PROJECT_DIR}/app/build/outputs/apk/release/app-release.apk"
FAST_APK_SRC="${PROJECT_DIR}/app/build/outputs/apk/debug/app-debug.apk"
RELEASE_APK_OUT="${OUT_DIR}/tiaosheng-counter-release.apk"
FAST_APK_OUT="${OUT_DIR}/tiaosheng-counter-fast.apk"

KEY_ALIAS="tiaosheng"
STORE_PASS="tiaosheng2024"
KEY_PASS="tiaosheng2024"
CERT_CN="Tiaosheng Counter"
CERT_OU="Dev"
CERT_O="Tiaosheng"
CERT_C="CN"

usage() {
    cat <<EOF
Usage:
  ./release.sh              Build signed release APK.
  ./release.sh fast_mode    Build debug APK quickly for development.

Aliases:
  fast, --fast, --fast-mode
EOF
}

FAST_MODE=false
case "${1:-}" in
    "")
        ;;
    "fast_mode" | "fast" | "--fast" | "--fast-mode")
        FAST_MODE=true
        ;;
    "-h" | "--help")
        usage
        exit 0
        ;;
    *)
        echo "ERROR: unknown argument: $1" >&2
        usage >&2
        exit 1
        ;;
esac

if [[ $# -gt 1 ]]; then
    echo "ERROR: too many arguments." >&2
    usage >&2
    exit 1
fi

JAVA_HOME="${JAVA_HOME:-/home/lwk/as/jdk-21}"
ANDROID_HOME="${ANDROID_HOME:-/home/lwk/as/android-sdk}"
ANDROID_SDK_ROOT="${ANDROID_SDK_ROOT:-${ANDROID_HOME}}"
export JAVA_HOME ANDROID_HOME ANDROID_SDK_ROOT
export PATH="${JAVA_HOME}/bin:${ANDROID_HOME}/platform-tools:${PATH}"

echo "============================================"
echo "   Tiaosheng Counter - Release Builder"
echo "============================================"
if [[ "${FAST_MODE}" == true ]]; then
    echo "   Mode: fast_mode (debug APK)"
else
    echo "   Mode: release"
fi
echo

KEYTOOL="${JAVA_HOME}/bin/keytool"
JAVA_BIN="${JAVA_HOME}/bin/java"
if [[ ! -x "${JAVA_BIN}" ]]; then
    echo "ERROR: java not found or not executable: ${JAVA_BIN}" >&2
    exit 1
fi
if [[ "${FAST_MODE}" != true && ! -x "${KEYTOOL}" ]]; then
    echo "ERROR: keytool not found or not executable: ${KEYTOOL}" >&2
    exit 1
fi
echo "JAVA_HOME=${JAVA_HOME}"

APKSIGNER=""
if [[ "${FAST_MODE}" != true && -d "${ANDROID_HOME}/build-tools" ]]; then
    BT_VER="$(find "${ANDROID_HOME}/build-tools" -mindepth 1 -maxdepth 1 -type d -printf '%f\n' | sort -Vr | head -n 1 || true)"
    if [[ -n "${BT_VER}" && -x "${ANDROID_HOME}/build-tools/${BT_VER}/apksigner" ]]; then
        APKSIGNER="${ANDROID_HOME}/build-tools/${BT_VER}/apksigner"
        echo "ANDROID_HOME=${ANDROID_HOME}"
        echo "BUILD_TOOLS=${BT_VER}"
    fi
fi

if [[ "${FAST_MODE}" != true && -z "${APKSIGNER}" ]]; then
    echo "WARNING: apksigner not found, signature verification skipped."
fi
echo

if [[ "${FAST_MODE}" == true ]]; then
    echo "[1/2] Building debug APK..."
    bash "${PROJECT_DIR}/gradlew" assembleDebug
    echo "Build successful."

    echo "[2/2] Copying APK to release directory..."
    mkdir -p "${OUT_DIR}"
    cp -f "${FAST_APK_SRC}" "${FAST_APK_OUT}"

    APK_OUT="${FAST_APK_OUT}"
else
    if [[ ! -f "${KEYSTORE}" ]]; then
        echo "[1/4] Generating keystore: ${KEYSTORE}"
        "${KEYTOOL}" -genkeypair -v \
            -keystore "${KEYSTORE}" \
            -alias "${KEY_ALIAS}" \
            -keyalg RSA -keysize 2048 -validity 10000 \
            -storepass "${STORE_PASS}" -keypass "${KEY_PASS}" \
            -dname "CN=${CERT_CN}, OU=${CERT_OU}, O=${CERT_O}, C=${CERT_C}"
        echo "Keystore created successfully."
    else
        echo "[1/4] Keystore already exists, skip."
    fi

    echo "[2/4] Writing keystore.properties"
    cat > "${PROPS}" <<EOF
storeFile=../tiaosheng.jks
storePassword=${STORE_PASS}
keyAlias=${KEY_ALIAS}
keyPassword=${KEY_PASS}
EOF
    echo "keystore.properties written."

    echo "[3/4] Building release APK..."
    bash "${PROJECT_DIR}/gradlew" assembleRelease
    echo "Build successful."

    echo "[4/4] Copying APK to release directory..."
    mkdir -p "${OUT_DIR}"
    cp -f "${RELEASE_APK_SRC}" "${RELEASE_APK_OUT}"

    APK_OUT="${RELEASE_APK_OUT}"
fi

echo
echo "============================================"
echo "   Build Complete"
echo "============================================"
SIZE_BYTES="$(stat -c '%s' "${APK_OUT}")"
SIZE_MB="$((SIZE_BYTES / 1048576))"
echo "Output: ${APK_OUT} (${SIZE_MB} MB)"
echo

if [[ "${FAST_MODE}" == true ]]; then
    echo "Signature verification skipped (fast_mode uses debug signing)."
elif [[ -n "${APKSIGNER}" ]]; then
    echo "Verifying signature..."
    if "${APKSIGNER}" verify --print-certs "${APK_OUT}"; then
        echo "Signature verified."
    else
        echo "WARNING: Signature verification failed"
    fi
else
    echo "Signature verification skipped (apksigner not found)."
fi

echo
echo "Done."
