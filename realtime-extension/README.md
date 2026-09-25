# Browser capture bridge

This is a permission-first scaffold for Chrome/Chromium.

It intentionally does **not** silently capture tabs or microphone audio.
A production implementation should:
1. show a RakshaCall consent screen;
2. request browser capture permission;
3. stream user-approved media over an authenticated channel;
4. display an always-visible capture indicator;
5. stop capture immediately when the user revokes consent.

For WhatsApp Web, this observes user-shared tab/media output rather than intercepting WhatsApp's encrypted network traffic.
