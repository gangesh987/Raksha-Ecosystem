# Deployment Guide

## Backend

Required:

```text
RAKSHA_API_TOKEN
RAKSHA_PUBLIC_WS_BASE
RAKSHA_DB_PATH
```

Optional:

```text
RAKSHA_RATE_LIMIT
RAKSHA_VERSION
WEBRTC_STUN_URL
WEBRTC_TURN_URL
WEBRTC_TURN_USERNAME
WEBRTC_TURN_CREDENTIAL
```

Run behind an HTTPS/WSS reverse proxy. Set `RAKSHA_PUBLIC_WS_BASE` to the public WSS base, for example `wss://api.example.com`.

## Docker

```bash
export RAKSHA_API_TOKEN='generate-a-long-random-secret'
export RAKSHA_PUBLIC_WS_BASE='wss://api.example.com'
docker compose -f server/docker-compose.yml up --build -d
```

Verify `/health` and `/ready` through the deployment endpoint.

## Android

Supply:

```text
RAKSHA_BACKEND_URL=https://api.example.com
RAKSHA_SIGNALING_WS_URL=wss://api.example.com/ws/call
RAKSHA_API_TOKEN=<same deployment credential>
```

Prefer CI/secret-store injection rather than committing these values.
