# Known Limitations

1. Android SDK/Gradle is not installed in the build environment, so APK/AAB compilation could not be verified.
2. Physical two-device WebRTC testing was not performed.
3. A real TURN service was not configured or tested.
4. Trusted-contact delivery remains provider-dependent; the backend does not falsely claim SMS/email/push delivery.
5. Authentication is a deployment bearer-token model, not a full per-user identity platform.
6. Device CPU/RAM/battery/WebRTC latency measurements were not collected.
7. External vulnerability/dependency scanners were not run.
8. Production TLS/certificate infrastructure must be configured by the deployment environment.
