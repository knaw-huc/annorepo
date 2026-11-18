#!/usr/bin/env bash
exec java -XX:-UseBASE64Intrinsics -XX:UseAVX=2 -jar annorepo-server.jar server config.yml
