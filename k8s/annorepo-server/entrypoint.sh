#!/bin/sh
# -UseBASE64Intrinsics / UseAVX=2: workarounds for java4shell
exec java \
  -XX:-UseBASE64Intrinsics \
  -XX:UseAVX=2 \
  -XX:MaxRAMPercentage=75 \
  -XX:+ExitOnOutOfMemoryError \
  $JAVA_OPTS \
  -jar annorepo-server.jar server config.yml "$@"
