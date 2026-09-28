FROM python:3.12-slim

WORKDIR /app

# Install system runtime dependencies for OpenCV, gRPC, and healthchecks
RUN apt-get update && apt-get install -y --no-install-recommends \
    curl \
    libgl1 \
    libglib2.0-0 \
    && rm -rf /var/lib/apt/lists/*

COPY requirements-deploy.txt .
RUN pip install --no-cache-dir -r requirements-deploy.txt

COPY . .

EXPOSE 8000 50051

HEALTHCHECK --interval=10s --timeout=5s --retries=5 --start-period=15s \
  CMD curl -f http://localhost:${PORT:-8000}/api/health || python -c "import urllib.request, os; port=os.environ.get('PORT','8000'); urllib.request.urlopen(f'http://localhost:{port}/api/health')" || exit 1

CMD ["sh", "-c", "uvicorn app.main:app --host 0.0.0.0 --port ${PORT:-8000}"]

