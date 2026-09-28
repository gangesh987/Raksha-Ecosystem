# Local / Physical Device Testing

## Physical Android devices

Do not use `10.0.2.2` for a physical phone. Replace the debug signaling endpoint in `CallViewModel` with the development computer's LAN IP, for example `ws://192.168.1.50:8000/ws/call`. Ensure both devices can reach port 8000.

## Test matrix

- Wi-Fi ↔ Wi-Fi
- Wi-Fi ↔ mobile data
- mobile data ↔ mobile data (requires publicly reachable signaling + TURN)
- camera toggle
- microphone mute
- front/rear camera
- call end
- temporary network interruption
- TURN relay

Phase 2 cannot honestly be called Internet-complete until the physical-device matrix has been exercised.
