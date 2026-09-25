# RakshaCall Production Deployment Guide

## 1. Build Flavors & Environments

The application supports three build configurations:

| Flavor / Type | Target Environment | Base URL | Minification / ProGuard |
| :--- | :--- | :--- | :--- |
| **Debug** | Local development / emulator | `http://10.0.2.2:8080` | Disabled |
| **Staging** | QA / internal test tracks | `https://staging-api.rakshacall.org` | Enabled |
| **Release** | Google Play Production Track | `https://api.rakshacall.org` | Enabled with R8 Shrinking |

## 2. Release Build Execution Commands

### Generate Debug APK:
```bash
./gradlew assembleDebug
```
Output: `app/build/outputs/apk/debug/app-debug.apk`

### Run Complete Unit Test Suite:
```bash
./gradlew testDebugUnitTest
```

### Generate Production Android App Bundle (AAB):
```bash
./gradlew bundleRelease
```
Output: `app/build/outputs/bundle/release/app-release.aab`

## 3. Production Signing Configuration
Place release keystore at `app/keystore/release.jks` and configure environment variables in CI/CD:
- `RAKSHA_KEYSTORE_PASSWORD`
- `RAKSHA_KEY_ALIAS`
- `RAKSHA_KEY_PASSWORD`
