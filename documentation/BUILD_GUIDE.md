# RAKSHA ECOSYSTEM — COMPLETE BUILD & COMPILATION GUIDE
**Version:** 1.0.0 Final Release  
**Target:** Independent Compilation of Two Android Applications and Backend Services  

---

## 1. System Requirements & Toolchain Prerequisites

| Requirement | Minimum Version | Recommended | Notes |
| :--- | :--- | :--- | :--- |
| **Operating System** | Windows 10/11, macOS 12+, Ubuntu 22.04+ | Windows 11 / Linux x86_64 | Multi-platform Gradle scripts |
| **Java Development Kit** | JDK 17 | Eclipse Temurin 17 (LTS) | Required by Android Gradle Plugin 8.7+ |
| **Android SDK** | API Level 35 (VanillaIceCream) | Build Tools 35.0.0 | Set via `ANDROID_HOME` or `local.properties` |
| **Python** | Python 3.10 | Python 3.11 / 3.12 | Backend virtualenv runtime |
| **Gradle** | 8.9 (via wrapper) | Gradle 8.9 Wrapper included | Zero manual Gradle install required |

---

## 2. Independent Android Project Builds

The ecosystem contains **two completely separate Android projects**. Each project has its own `build.gradle.kts`, `settings.gradle.kts`, and Gradle wrapper.

### Build 1: Raksha Video (`com.raksha.video`)

1. Open a terminal in the `RakshaVideo` directory:
   ```powershell
   cd RakshaVideo
   ```
2. Verify SDK location in `local.properties`:
   ```properties
   sdk.dir=C\:\\Users\\<YourUser>\\AppData\\Local\\Android\\Sdk
   ```
3. Execute clean unit tests and build the Debug APK:
   ```powershell
   .\gradlew.bat assembleDebug
   ```
4. Output Artifact:
   - **Debug APK:** `RakshaVideo/app/build/outputs/apk/debug/app-debug.apk`
   - **Package ID:** `com.raksha.video`
   - **Application Label:** `Raksha Video`

---

### Build 2: RakshaCall (`com.rakshacall.safety`)

1. Open a terminal in the `RakshaCall` directory:
   ```powershell
   cd RakshaCall
   ```
2. Verify SDK location in `local.properties`:
   ```properties
   sdk.dir=C\:\\Users\\<YourUser>\\AppData\\Local\\Android\\Sdk
   ```
3. Run the complete automated test suite (119 unit tests):
   ```powershell
   .\gradlew.bat testDebugUnitTest
   ```
4. Build the Debug and Release APKs:
   ```powershell
   .\gradlew.bat assembleDebug
   .\gradlew.bat assembleRelease
   ```
5. Output Artifacts:
   - **Debug APK:** `RakshaCall/app/build/outputs/apk/debug/app-debug.apk`
   - **Release APK:** `RakshaCall/app/build/outputs/apk/release/app-release.apk`
   - **Package ID:** `com.rakshacall.safety`
   - **Application Label:** `RakshaCall`

---

## 3. Simultaneous Installation on Physical Android Device

Both applications have distinct package identifiers and application labels. They can and should be installed side-by-side on the same handheld Android phone.

```powershell
# 1. Verify device connection
adb devices

# 2. Install Raksha Video
adb install -r APK/RakshaVideo-debug.apk

# 3. Install RakshaCall
adb install -r APK/RakshaCall-debug.apk

# 4. Verify simultaneous installation
adb shell pm list packages | Select-String "raksha"
```

**Expected ADB Output:**
```text
package:com.raksha.video
package:com.rakshacall.safety
```

---

## 4. Backend Service Startup

To start the unified backend serving both applications:

```powershell
cd backend
$env:PYTHONPATH="."
python -m uvicorn app.main:app --host 0.0.0.0 --port 8000
```
- **FastAPI Control Plane:** `http://localhost:8000`
- **WebRTC Signaling Route:** `ws://localhost:8000/ws/signaling`
- **Real-Time Safety WebSocket:** `ws://localhost:8000/api/ws/sessions/{sid}`
