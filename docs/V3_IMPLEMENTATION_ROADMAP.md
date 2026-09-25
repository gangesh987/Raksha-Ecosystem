# V3 implementation roadmap

## Already scaffolded
1. Adaptive risk fusion with trajectory/acceleration.
2. Scam tactic extraction.
3. Signal quality/confidence separation.
4. Counterfactual safety explanations.
5. Evidence hash-chain primitive.
6. Advanced frontend feature panel.
7. Automated backend tests.
8. Model-adapter architecture boundary.

## Next production integrations
- Local/edge ASR model with multilingual support.
- Real trained scam-language classifier.
- ONNX visual consistency adapter.
- Experimental rPPG adapter.
- PostgreSQL + Alembic migration execution.
- Redis session state.
- Real notification provider behind consent + delivery receipts.
- PDF evidence renderer with redaction.
- OpenTelemetry/Prometheus metrics.
- CI security/test pipeline.
- Android foreground-service capture using explicit permissions.

## Important product boundary
Never describe demo adapters, simulated notifications, or experimental liveness as production-grade detection. The UI should expose provider/model status so a demo cannot be mistaken for a live deployment.
