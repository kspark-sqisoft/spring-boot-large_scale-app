#!/usr/bin/env sh
set -eu
cd /app
chmod +x ./gradlew 2>/dev/null || true
exec ./gradlew --continuous bootRun
