#!/bin/sh
set -e
if command -v gradle >/dev/null 2>&1; then exec gradle "$@"; fi
echo "Gradle is not installed and gradle-wrapper.jar is unavailable in this checkout." >&2
exit 127

