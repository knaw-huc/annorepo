#!/usr/bin/env bash
if [[ -z "${AR_ADMIN_BACKEND}" ]]; then
  echo "environment variable AR_ADMIN_BACKEND not defined!"
else
  ./wait-for-it.sh ${AR_ADMIN_BACKEND/http:\/\//} --timeout=0
  curl -X POST $AR_ADMIN_BACKEND/tasks/recalculate-field-count
fi
