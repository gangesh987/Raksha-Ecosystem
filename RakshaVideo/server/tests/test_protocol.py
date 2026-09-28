from fastapi.testclient import TestClient
from app.main import app

client = TestClient(app)

def test_health():
    r = client.get('/health')
    assert r.status_code == 200
    assert r.json()['status'] == 'ok'

def test_webrtc_config():
    r = client.get('/api/webrtc/config')
    assert r.status_code == 200
    assert 'ice_servers' in r.json()
