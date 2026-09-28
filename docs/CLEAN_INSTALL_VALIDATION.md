# CLEAN INSTALLATION & DEPENDENCY AUDIT VALIDATION
**Mission**: Technova 2026 Grand Finale Stability & Reproducibility Audit  
**Phase**: Step 2 — Clean Environment Verification  
**Date**: 2026-09-26  
**Status**: **GREEN — VERIFIED REPRODUCIBLE**

---

## 1. Clean Environment Specifications

* **Virtual Environment Path**: `$env:TEMP\raksha_clean_venv` (outside active workspace)
* **Base Python**: `Python 3.11.0 (main, Oct 24 2022, 18:26:48) [MSC v.1933 64 bit (AMD64)]`
* **Virtualenv Pip Initial**: `pip 22.3` (bundled with Python 3.11.0 `ensurepip`)
* **Virtualenv Pip Upgraded**: `pip 26.2.1` (to support non-blocking wheel installation & PEP 668/resolvelib backtracking on Windows)
* **Target Requirements File**: [`backend/requirements.txt`](file:///c:/Users/gangs/Downloads/APP%20OF%20RAKSHA/backend/requirements.txt)
* **Initial Requirements SHA-256**: `C7B07F014613104CAE6ECC83153511F75DE3E7DF2114A1F4FB8C049B4BFC9196`
* **Final Requirements SHA-256**: `F3063B11A2BDF2FFF9D2972DA8EB73C45329F84896D2E21E47D364D7FC3404D6`

---

## 2. Installation Command & Initial Failure Root Cause

### Installation Command
```powershell
# In a fresh virtual environment:
python -m venv $env:TEMP\raksha_clean_venv
$env:TEMP\raksha_clean_venv\Scripts\python.exe -m pip install -r backend/requirements.txt
```

### Initial Problem Found & Diagnostic Analysis
1. **gRPC Dependencies**:
   * `grpcio>=1.62.0`, `grpcio-tools>=1.62.0`, and `protobuf>=4.25.0` were already declared in `backend/requirements.txt`.
   * Installed successfully: `grpcio==1.84.0`, `protobuf==7.36.2`, `grpcio-tools==1.84.0`.
   * Protobuf imports verified: `from app.grpc_gen import pb2, pb2_grpc` works cleanly.

2. **The `bcrypt` / `passlib` Regression on Clean Installs**:
   * `backend/requirements.txt` previously declared `passlib[bcrypt]==1.7.4` without pinning `bcrypt`.
   * On a fresh install in 2026, `pip` resolves `passlib[bcrypt]` by pulling `bcrypt 5.0.0` (or `bcrypt >= 4.1.0`).
   * **Root Cause Trace**:
     * In `bcrypt >= 4.1.0` and `5.0.0`, `bcrypt.hashpw` enforces an unyielding check: `ValueError: password cannot be longer than 72 bytes`.
     * `passlib 1.7.4` (unmaintained upstream) attempts to detect an ancient wrap bug during backend initialization (`detect_wrap_bug` in `passlib/handlers/bcrypt.py`) using a 255-byte secret:
       ```python
       secret = (b"0123456789"*26)[:255]
       if verify(secret, bug_hash):
       ```
     * When `bcrypt >= 4.1.0` is present, this 255-byte test secret causes `bcrypt` to immediately raise `ValueError: password cannot be longer than 72 bytes` **even during the first authentication call of a short, normal password**!
     * This caused `backend/tests/test_auth_and_sessions.py` to fail on both normal registration/login and long password tests.

---

## 3. Changes Made

1. **Pinned `bcrypt==4.0.1` in `backend/requirements.txt`**:
   * `bcrypt 4.0.1` is the exact version running in the verified base environment that properly inter-operates with `passlib 1.7.4`.
   * Added `bcrypt==4.0.1` immediately following `passlib[bcrypt]==1.7.4`.
2. **Pip Resolver Upgrade Recommendation**:
   * Note for deployment: Standard Python 3.11 `ensurepip` ships with `pip 22.3` (from 2022). Upgrading pip via `pip install --upgrade pip` is strongly recommended for clean installs to avoid legacy Windows terminal deadlocks during wheel unpacking.

---

## 4. Final Clean-Install Verification Results

All tests executed inside `$env:TEMP\raksha_clean_venv` using `$env:PYTHONPATH="backend"`:

| Verification Stage | Command Executed | Outcome | Details / Metrics |
| :--- | :--- | :--- | :--- |
| **gRPC & Protobuf Import** | `python -c "import grpc, google.protobuf..."` | **PASSED** | `grpcio 1.84.0`, `protobuf 7.36.2`, `pb2_grpc.ProtectionServiceServicer: True` |
| **Backend Test Collection** | `pytest backend/tests/ --collect-only` | **PASSED** | **25 / 25 tests collected**, 0 collection errors in 34.9s |
| **Backend Unit Tests** | `pytest backend/tests/ -v` | **PASSED** | **25 / 25 PASSED**, 0 failed, 1 warning (starlette deprecation) in 34.5s |
| **ML Engine Tests** | `pytest tests/ -v` | **PASSED** | **17 / 17 PASSED** in 10.58s (`test_jev_integration.py` 7/7, `test_scam_classifier.py` 10/10) |
| **FastAPI Live Health Check** | `TestClient(app).get("/api/health")` | **PASSED** | Status code `200 OK`, `{"status": "ok", "service": "rakshacall"}` |
| **gRPC Live Stream/Liveness** | `ProtectionServiceClient.check_liveness()` | **PASSED** | `is_ready: True`, `active_model_mode: grpc.aio+multilingual...` |

---

## 5. Remaining Limitations
* `passlib` remains pinned at `1.7.4`. Future migration to direct `bcrypt` or `argon2-cffi` is recommended on the post-hackathon roadmap to eliminate `passlib` legacy workarounds entirely.
* Clean installation requires internet access to download wheels unless pre-cached wheels are supplied.
