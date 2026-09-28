# Android Release Signing

1. Create a release keystore outside the repository.
2. Store keystore credentials in your CI secret store or local Gradle user properties.
3. Never commit `.jks`, passwords, or signing keys.
4. Configure the `signingConfigs` block only in the deployment environment.
5. Verify the release artifact with `apksigner verify --verbose`.

This repository intentionally contains no signing secrets.
