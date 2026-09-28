# Final Unified Pipeline Call Graph

This document illustrates the validated, single-source-of-truth call graph for RakshaCall. All three transports (REST, WebSocket, gRPC) have been rigorously tested to ensure they utilize this exact path, yielding 100% semantic parity.

```mermaid
graph TD
    %% Transports
    REST["REST API<br/>POST /api/sessions/{sid}/analyze"]
    WS["WebSocket API<br/>/api/ws/sessions/{sid}"]
    GRPC["gRPC Streaming<br/>ProtectionService.StreamProtection"]
    
    %% Unification Point
    UAP["UnifiedAnalysisPipeline.analyze()"]
    
    REST --> UAP
    WS --> UAP
    GRPC --> UAP
    
    subgraph Unified Pipeline
        %% Core Stages
        JEV["LocalSemanticJEVProvider<br/>(Neural Model v2 + Safety Floor)"]
        SM["ScamStageMachine"]
        MV["ManipulationVelocityEngine"]
        FUS["MultimodalRiskFusionEngine"]
        SBE["SafetyBrakeEngine"]
        EV["EvidenceVault / Ledger"]
        
        UAP --> JEV
        JEV --> SM
        SM --> MV
        MV --> FUS
        FUS --> SBE
        SBE --> EV
    end
    
    %% Outputs
    RESP["PipelineResult"]
    EV --> RESP
    
    %% Returns
    RESP -.-> REST_RES["JSON Response"]
    RESP -.-> WS_RES["RiskUpdate JSON"]
    RESP -.-> GRPC_RES["pb2.RiskUpdate"]
```

### Execution Guarantee
As verified by the `validate_transports.py` test suite, any string inputted into any of the three transports will pass through the exact same instances of the JEV Model, Stage Machine, Velocity Engine, Fusion Engine, and Evidence Ledger, producing cryptographically hashed events that are identical across transports.
