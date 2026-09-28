# RakshaCall Android Project
**Package ID:** `com.rakshacall.safety`  
**Application Label:** `RakshaCall`  
**Version:** 1.0.0 Production  

## Overview
RakshaCall is the dedicated safety and fraud defense application within the Raksha Ecosystem. It provides ambient scam classification, manipulation velocity tracking, full-screen Safety Brake interlocks, and cryptographic evidence chain logging.

## Build Instructions
```powershell
# Run unit tests
.\gradlew.bat testDebugUnitTest

# Assemble Debug APK
.\gradlew.bat assembleDebug

# Assemble Release APK
.\gradlew.bat assembleRelease
```

## Integration with Raksha Video
- Connects to the shared Raksha backend at `https://poor-keys-like.loca.lt` (or local LAN `http://172.17.35.95:8000`).
- Receives protection sessions and deep-link handoffs from Raksha Video via `rakshacall://protection/session?id={session_id}&call={call_id}`.
