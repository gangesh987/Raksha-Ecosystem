# RAKSHA ECOSYSTEM — PRODUCTION CLOUD & LAN DEPLOYMENT GUIDE
**Version:** 1.0.0 Final Release  
**Scope:** Deployment Architecture for the Shared Raksha Backend  

---

## 1. Cloud Architecture Overview

The backend acts as the single unified control plane and intelligence hub for both Android clients:

```text
  📱 Raksha Video                              📱 RakshaCall
(com.raksha.video)                          (com.rakshacall.safety)
        │                                              │
        │ WSS (WebRTC Signaling)                       │ HTTPS (Auth, REST)
        │ HTTPS (ICE config, Calls)                    │ WSS (Risk Stream)
        └──────────────────────┬───────────────────────┘
                               │ TLS (Port 443)
                               ▼
               ┌───────────────────────────────┐
               │    Edge Proxy / CDN Router    │
               │ (Cloudflare / Render / Edge)  │
               └───────────────┬───────────────┘
                               │ Forward to Port 8000
                               ▼
               ┌───────────────────────────────┐
               │    Unified FastAPI Backend    │
               │  app.main:app (Port 8000)     │
               └───────────────┬───────────────┘
                               │
                ┌──────────────┴──────────────┐
                ▼                             ▼
       SQLite / PostgreSQL           UnifiedAnalysisPipeline
       (Auth & Evidence Vault)       (Scam Classifier & Velocity)
```

---

## 2. Environment Variables Inventory

| Variable Name | Default Value | Purpose | Required in Prod |
| :--- | :--- | :--- | :---: |
| `PORT` | `8000` | Port for the Uvicorn HTTP/WebSocket server | Yes (Dynamic on Render/Cloud Run) |
| `RAKSHA_ENV` | `production` | Deployment environment flag | Yes |
| `SECRET_KEY` | *(Generated random)* | JWT signing key for authentication tokens | Yes |
| `DATABASE_URL` | `sqlite:///./rakshacall.db` | Primary database connection string | Recommended PostgreSQL in Prod |
| `CORS_ORIGINS` | `*` | Allowed CORS origins for web portals | Yes |
| `RAKSHA_RATE_LIMIT` | `120` | Max requests per minute per IP | Optional |
| `WEBRTC_STUN_URL` | `stun:stun.l.google.com:19302` | Public STUN server for ICE negotiation | Yes |
| `WEBRTC_TURN_URL` | `""` | TURN relay server URL for symmetric NATs | Optional (Recommended for cellular) |

---

## 3. Deployment Methods

### Option A: Render.com Blueprint Deployment (`render.yaml`)
1. Connect your Git repository to Render.
2. Render automatically discovers `render.yaml`.
3. Build command:
   ```bash
   pip install -r backend/requirements-deploy.txt
   ```
4. Start command:
   ```bash
   uvicorn app.main:app --host 0.0.0.0 --port $PORT
   ```

### Option B: Docker Container Deployment
```bash
docker build -t raksha-backend:1.0.0 -f backend/Dockerfile .
docker run -d -p 8000:8000 --env-file .env raksha-backend:1.0.0
```

### Option C: Instant Local LAN Deployment (Demo Mode)
```powershell
cd backend
$env:PYTHONPATH="."
python -m uvicorn app.main:app --host 0.0.0.0 --port 8000
```
- Bound to all local network interfaces.
- Accessible on LAN via `http://<Host-IP>:8000` (e.g. `http://172.17.35.95:8000`).

---

## 4. Health & Operational Verification

Verify backend readiness before starting demonstrations:

```bash
# Public HTTPS Health Check
curl -s -H "Bypass-Tunnel-Reminder: true" https://poor-keys-like.loca.lt/api/health

# WebRTC ICE Configuration Check
curl -s https://poor-keys-like.loca.lt/api/webrtc/config
```

**Expected JSON Response:**
```json
{
  "status": "healthy",
  "service": "RakshaCall Safety API",
  "version": "1.0.0",
  "transports": {
    "rest": "operational",
    "websocket": "operational",
    "grpc": "internal_ipc"
  }
}
```
