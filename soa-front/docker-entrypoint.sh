#!/bin/sh
set -eu

# The host key stays mode 600. Copy it into the container for the app user.
install -d -o node -g node -m 700 /run/frontend-tls
install -o node -g node -m 600 /certs/service/tls.key /run/frontend-tls/tls.key
exec setpriv --reuid=node --regid=node --init-groups "$@"
