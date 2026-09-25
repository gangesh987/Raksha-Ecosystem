from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware
from contextlib import asynccontextmanager
from .config import settings
from .db import init_db
from .api.auth import router as auth_router
from .api.sessions import router as session_router
from .realtime_routes import router as realtime_router

import asyncio
from .grpc_service import serve_grpc

@asynccontextmanager
async def lifespan(app):
    init_db()
    grpc_server = None
    try:
        grpc_server = await serve_grpc(port=50051)
    except Exception as e:
        print(f"[gRPC] Server start warning (e.g. port bound): {e}")
    yield
    if grpc_server:
        await grpc_server.stop(grace=1.0)

app=FastAPI(title="RakshaCall Security API",version="1.0.0",lifespan=lifespan)
app.add_middleware(CORSMiddleware,allow_origins=[x.strip() for x in settings.cors_origins.split(",")],allow_credentials=True,allow_methods=["*"],allow_headers=["*"])
app.include_router(auth_router)
app.include_router(session_router)
app.include_router(realtime_router)

@app.get("/api/health")
def health(): return {"status":"ok","service":"rakshacall","model_mode":settings.model_mode}
