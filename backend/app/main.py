"""
RakshaCall Security API — Main Application Entry Point.
Initializes FastAPI, database, gRPC server, and AI pipeline.
"""

import logging
import sys
from contextlib import asynccontextmanager
from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware
from .config import settings
from .db import init_db

# Configure structured logging
logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s | %(name)s | %(levelname)s | %(message)s",
    stream=sys.stdout,
)
logger = logging.getLogger("rakshacall")


@asynccontextmanager
async def lifespan(app):
    """Application lifecycle: init DB, start gRPC, warm up AI pipeline."""
    # Initialize database tables
    init_db()
    logger.info("[Startup] Database initialized")

    # Warm up unified AI pipeline (loads neural model)
    try:
        from .ai.unified_pipeline import get_pipeline
        pipeline = get_pipeline()
        logger.info(f"[Startup] AI Pipeline initialized: {pipeline.jev_provider.provider_name}")
    except Exception as e:
        logger.warning(f"[Startup] AI Pipeline warm-up warning: {e}")

    # Start gRPC server
    grpc_server = None
    try:
        from .grpc_service import serve_grpc
        grpc_server = await serve_grpc(port=50051)
        logger.info("[Startup] gRPC server started on port 50051")
    except Exception as e:
        logger.warning(f"[Startup] gRPC server start warning: {e}")

    yield

    # Shutdown
    if grpc_server:
        await grpc_server.stop(grace=1.0)
        logger.info("[Shutdown] gRPC server stopped")


app = FastAPI(
    title="RakshaCall Security API",
    description="AI-powered conversation safety platform for detecting social-engineering scams",
    version="2.0.0",
    lifespan=lifespan,
)

# CORS middleware
app.add_middleware(
    CORSMiddleware,
    allow_origins=[x.strip() for x in settings.cors_origins.split(",")],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# Import and include routers
from .api.auth import router as auth_router
from .api.sessions import router as session_router
from .realtime_routes import router as realtime_router
from .api.webrtc_signaling import router as webrtc_router

app.include_router(auth_router)
app.include_router(session_router)
app.include_router(realtime_router)
app.include_router(webrtc_router)


@app.get("/api/health")
def health():
    """Health check endpoint with model and pipeline status."""
    pipeline_status = "unknown"
    model_info = {}
    try:
        from .ai.unified_pipeline import get_pipeline
        pipeline = get_pipeline()
        pipeline_status = "ready"
        model_info = {
            "provider": pipeline.jev_provider.provider_name,
        }
        # Check if neural classifier is loaded
        if hasattr(pipeline.jev_provider, 'neural_classifier') and pipeline.jev_provider.neural_classifier:
            nc = pipeline.jev_provider.neural_classifier
            model_info["neural_model_loaded"] = nc.is_loaded
            model_info["model_version"] = nc.model_version if nc.is_loaded else "not loaded"
            model_info["parameter_count"] = nc.parameter_count if nc.is_loaded else 0
    except Exception as e:
        pipeline_status = f"error: {str(e)[:100]}"

    return {
        "status": "ok",
        "service": "rakshacall",
        "version": "2.0.0",
        "pipeline_status": pipeline_status,
        "model": model_info,
    }
