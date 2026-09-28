# Final Security Audit

## Controls implemented/reviewed
- No committed production API token.
- Protected API routes require `RAKSHA_API_TOKEN`.
- Production Android configuration requires HTTPS/WSS and a non-empty token.
- WebSocket signaling requires authentication.
- WebSocket messages are protocol-version checked.
- Call rooms are limited to two participants.
- Request rate limiting is applied to HTTP routes.
- Safety actions require explicit confirmation and support idempotency keys.
- Evidence exports do not include raw media by default.
- Production Docker runs as a non-root user.
- Docker health check added.
- Secrets are represented by environment/deployment configuration.

## Residual risks
- The current authentication model is a deployment-level bearer token rather than a complete multi-user identity/refresh-token system.
- Call/protection session ownership is not yet backed by per-user authorization records.
- WebSocket rate limiting is not independently implemented at the connection/message layer.
- Production TLS termination and certificate management require deployment infrastructure.
- External dependency vulnerability scanning was not available in this environment.

These are documented limitations, not claims of complete security assurance.
