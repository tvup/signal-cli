# syntax=docker/dockerfile:1.7
FROM bbernhard/signal-cli-rest-api:latest

ENV MODE=json-rpc

COPY dist/signal-cli-*-SNAPSHOT.tar.gz /tmp/signal-cli.tar.gz

RUN set -eux; \
    mkdir -p /opt/signal-cli-custom; \
    tar -xzf /tmp/signal-cli.tar.gz -C /opt/signal-cli-custom --strip-components=1; \
    ORIG=$(command -v signal-cli || echo /usr/bin/signal-cli); \
    ln -sf /opt/signal-cli-custom/bin/signal-cli "$ORIG"; \
    "$ORIG" --version; \
    rm -f /tmp/signal-cli.tar.gz

# Wrap jsonrpc2-helper, så confen patches lige efter den er genereret ved container-start
RUN mv /usr/bin/jsonrpc2-helper /usr/bin/jsonrpc2-helper.real && \
    printf '%s\n' \
        '#!/bin/sh' \
        'set -e' \
        '/usr/bin/jsonrpc2-helper.real "$@"' \
        'for f in /etc/supervisor/conf.d/signal-cli-json-rpc-*.conf; do' \
        '  [ -f "$f" ] || continue' \
        '  sed -i "s| daemon | --service-environment=test daemon |" "$f"' \
        '  echo "Patched $f for --service-environment=test"' \
        'done' \
        > /usr/bin/jsonrpc2-helper && \
    chmod +x /usr/bin/jsonrpc2-helper
