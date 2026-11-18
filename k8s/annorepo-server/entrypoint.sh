#!/bin/sh
exec java -XX:UseAVX=2 -XX:-UseBASE64Intrinsics -XX:MaxRAMPercentage=75 -jar annorepo-server.jar server config.yml
