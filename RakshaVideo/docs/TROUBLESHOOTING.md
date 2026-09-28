# Troubleshooting

## Signaling connected but media does not connect

Check ICE candidate exchange and TURN configuration. WebSocket connectivity does not mean WebRTC media connectivity.

## Physical phone cannot connect

Use the computer LAN IP, not `10.0.2.2`. Confirm firewall rules and that the server binds to `0.0.0.0`.

## Camera permission

Grant CAMERA and RECORD_AUDIO before joining. Android permissions are runtime controlled.

## NAT/mobile networks

STUN may not be enough. Configure TURN and verify that the server returns valid relay credentials.

## No remote video

Check that the remote `VideoTrack` is added to the renderer and that the remote SDP/ICE exchange completed.
