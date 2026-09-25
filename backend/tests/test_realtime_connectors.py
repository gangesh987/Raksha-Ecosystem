import pytest
from fastapi.testclient import TestClient
from app.main import app
from app.realtime_connectors import MeetMediaConnector, UserConsentedCaptureConnector, ConnectorState

client = TestClient(app)

@pytest.mark.asyncio
async def test_connectors_require_consent():
    m = MeetMediaConnector()
    w = UserConsentedCaptureConnector()
    assert (await m.status()).state == ConnectorState.PERMISSION_REQUIRED
    assert (await w.status()).state == ConnectorState.PERMISSION_REQUIRED

def test_realtime_connectors_endpoint():
    res = client.get("/api/realtime/connectors")
    assert res.status_code == 200
    data = res.json()
    assert "connectors" in data
    assert len(data["connectors"]) >= 2
    assert "policy" in data
    assert "Explicit user consent required" in data["policy"]

def test_warning_endpoint():
    # Health and public API check
    health_res = client.get("/api/health")
    assert health_res.status_code == 200
    assert health_res.json()["status"] == "ok"
