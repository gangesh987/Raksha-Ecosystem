# REAL-TIME gRPC STREAMING ARCHITECTURE SPECIFICATION
## Bidirectional HTTP/2 Protocol Buffers Transport for Mission-Critical Conversation Safety
**Document Version:** 3.0.0-PROD  
**Target Protocol:** gRPC v1.60+ / HTTP/2 / Protocol Buffers v3  
**Service Interface:** `rakshacall.protection.v1.ProtectionService`  

---

## 1. WHY gRPC OVER WEBSOCKETS FOR AI SAFETY

RakshaCall strictly replaces WebSockets for its primary real-time intelligence data plane with **gRPC over HTTP/2**. 

In high-stakes, life-critical AI safety interventions, WebSockets introduce unacceptable architectural liabilities:

| Architectural Metric | WebSockets (Legacy Prototype) | gRPC / HTTP/2 (Production RakshaCall) | Safety Impact |
|:---|:---|:---|:---|
| **Schema Contract** | Ad-hoc text JSON; vulnerable to runtime type mismatches | Strongly-typed Protocol Buffers (`.proto`) compiled to native code | Eliminates schema parsing crashes during live scam interception |
| **Serialization Overhead** | Text JSON stringification and parsing (~15–30% CPU overhead) | Binary wire format (Varint encoding, compact byte arrays) | Reduces serialization latency by 4x–8x on mobile hardware |
| **Multiplexing** | One logical stream per TCP connection; head-of-line blocking | True HTTP/2 stream multiplexing over a single TCP/TLS connection | Concurrent audio chunks, video frames, and risk events without blocking |
| **Flow Control & Backpressure** | Application-level manual buffering; risk of memory exhaustion | Native HTTP/2 stream-level and connection-level window flow control | Automatic backpressure prevents mobile buffer overflow on weak networks |
| **Error Propagation** | Generic close codes (1000–1011); custom error payloads | Standardized rich gRPC status codes (`UNAVAILABLE`, `DEADLINE_EXCEEDED`, etc.) | Deterministic failure recovery and immediate fallback activation |
| **Bidirectional Streaming** | Supported, but requires manual framing | Native, language-idiomatic bidirectional async generators (`grpc.aio`) | Clean reactive reactive dataflow on both client and backend |

---

## 2. PROTOCOL BUFFER SPECIFICATION

The production schema is standardized in `proto/protection_service.proto`:

```protobuf
syntax = "proto3";

package rakshacall.protection.v1;

option java_package = "com.rakshacall.safety.grpc";
option java_multiple_files = true;
option go_package = "github.com/rakshacall/proto/v1;rakshacallv1";

// Primary Real-Time Protection Streaming Service
service ProtectionService {
  // Bidirectional streaming channel for live call safety analysis
  rpc StreamProtection(stream ClientFrame) returns (stream RiskUpdate);

  // Unary health and model readiness check
  rpc CheckLiveness(LivenessRequest) returns (LivenessResponse);
  
  // Explicit session termination and summary generation
  rpc EndSession(EndSessionRequest) returns (EndSessionResponse);
}

// Media frame payload originating from the user's mobile device
message ClientFrame {
  string session_id = 1;
  uint64 sequence_number = 2;
  int64 client_timestamp_ms = 3;
  
  // Consented media chunk
  oneof media_payload {
    bytes audio_pcm16 = 4;        // 16kHz, 16-bit Mono Little-Endian PCM (250ms chunks)
    string transcript_snippet = 5; // Local edge ASR or manual text fragment
    bytes video_frame_jpeg = 6;   // 640x640 JPEG compressed frame (1-2 fps)
  }
  
  string detected_language = 7;   // ISO-639-1 (e.g., "ta", "en", "hi", "ta-Latn")
  DeviceContext device_context = 8;
  bool user_consent_active = 9;   // Explicit legal consent flag; required to process
}

// Device hardware and sensor signals
message DeviceContext {
  enum AudioRouting {
    EARPIECE = 0;
    SPEAKERPHONE = 1;
    WIRED_HEADSET = 2;
    BLUETOOTH_SCO = 3;
  }
  
  AudioRouting audio_route = 1;
  bool screen_locked = 2;
  bool is_foreground_service = 3;
  float battery_level = 4;
  string network_type = 5; // "WIFI", "4G", "5G", "DEGRADED"
}

// Risk update streamed back from backend to mobile client
message RiskUpdate {
  string session_id = 1;
  uint64 ack_sequence_number = 2;
  int64 server_timestamp_ms = 3;
  
  uint32 risk_score = 4; // 0 to 100
  RiskLevel risk_level = 5;
  ScamStage scam_stage = 6;
  VelocityLevel manipulation_velocity = 7;
  
  repeated TacticEvidence detected_tactics = 8;
  bool safety_brake_triggered = 9;
  InterventionPlan intervention = 10;
  
  string primary_explanation = 11;
  repeated string contributing_factors = 12;
  
  // Supporting visual signals (Contextual only)
  VisualPerceptionContext visual_context = 13;
  
  // Model disagreement indicator
  bool model_disagreement = 14;
  string disagreement_reason = 15;
  
  // Tamper-evident ledger hash for this event block
  string current_block_hash = 16;
}

enum RiskLevel {
  RISK_LEVEL_UNSPECIFIED = 0;
  RISK_LEVEL_LOW = 1;       // 0 - 30
  RISK_LEVEL_MEDIUM = 2;    // 31 - 60
  RISK_LEVEL_HIGH = 3;      // 61 - 80
  RISK_LEVEL_CRITICAL = 4;  // 81 - 100
}

enum ScamStage {
  STAGE_UNSPECIFIED = 0;
  STAGE_CONTACT = 1;
  STAGE_AUTHORITY = 2;
  STAGE_FEAR = 3;
  STAGE_ISOLATION = 4;
  STAGE_DEMAND = 5;
  STAGE_PAYMENT_CREDENTIAL = 6;
  STAGE_CRITICAL_BRAKE = 7;
}

enum VelocityLevel {
  VELOCITY_UNSPECIFIED = 0;
  VELOCITY_LOW = 1;
  VELOCITY_MODERATE = 2;
  VELOCITY_HIGH = 3;
}

message TacticEvidence {
  string tactic_id = 1;        // "AUTHORITY", "FEAR", "URGENCY", "PAYMENT", etc.
  string display_name = 2;
  float probability = 3;       // 0.0 - 1.0
  float confidence = 4;        // 0.0 - 1.0
  string evidence_quote = 5;   // Exact transcript excerpt
  int64 timestamp_ms = 6;
  string language = 7;
  string source = 8;           // "SPEECH", "VISUAL", "SYSTEM"
  bool is_irreversible = 9;    // Payment, OTP, PIN, Remote Access
  string explanation = 10;
}

message InterventionPlan {
  enum ActionType {
    NONE = 0;
    SHOW_WARNING = 1;
    ENGAGE_SAFETY_BRAKE = 2;
    LAUNCH_VERIFICATION_COACH = 3;
    NOTIFY_TRUSTED_CONTACT = 4;
  }
  
  ActionType primary_action = 1;
  string localized_audio_alert_key = 2; // e.g., "ta_stop_payment", "en_stop_otp"
  string display_headline = 3;
  string display_subtext = 4;
  repeated string guidance_steps = 5;
}

message VisualPerceptionContext {
  int32 person_count = 1;
  bool secondary_device_present = 2;
  bool document_or_screen_present = 3;
  float visual_confidence = 4;
  string contextual_note = 5;
}

message LivenessRequest {
  string client_version = 1;
}

message LivenessResponse {
  bool is_ready = 1;
  string active_model_mode = 2;
  repeated string supported_languages = 3;
  int64 server_time_ms = 4;
}

message EndSessionRequest {
  string session_id = 1;
  string termination_reason = 2; // "USER_DISCONNECTED", "SAFETY_BRAKE_EXIT", "NORMAL_COMPLETION"
}

message EndSessionResponse {
  string session_id = 1;
  uint32 peak_risk_score = 2;
  string final_risk_level = 3;
  uint32 total_tactics_detected = 4;
  string head_evidence_hash = 5;
  int64 duration_seconds = 6;
}
```

---

## 3. STREAM LIFECYCLE & PROTOCOL STATE MACHINE

```
      [CLIENT]                                                 [BACKEND gRPC]
         |                                                           |
         |----------------- 1. Establish HTTP/2 TLS ---------------->|
         |                                                           |
         |---- 2. ClientFrame(seq=1, consent=true, audio_chunk) ---->|
         |                                                           |
         |                                     [Pipeline Processes]  |
         |                                     [ASR -> Intent ->     |
         |                                      Tactics -> Risk]     |
         |                                                           |
         |<--- 3. RiskUpdate(ack=1, score=15, stage=CONTACT) --------|
         |                                                           |
         |---- 4. ClientFrame(seq=2, consent=true, audio_chunk) ---->|
         |                                                           |
         |                                     [Authority + Fear     |
         |                                      + Urgency detected]  |
         |                                                           |
         |<--- 5. RiskUpdate(ack=2, score=72, stage=FEAR) -----------|
         |                                                           |
         |---- 6. ClientFrame(seq=3, "transfer to escrow") --------->|
         |                                                           |
         |                                     [Payment Demand +     |
         |                                      Irreversible Action] |
         |                                                           |
         |<--- 7. RiskUpdate(score=88, SAFETY_BRAKE_TRIGGERED) ------|
         |                                                           |
   [SAFETY BRAKE]                                                    |
 [VIBRATION + AUDIO]                                                 |
         |                                                           |
         |---- 8. EndSessionRequest(session_id) -------------------->|
         |<--- 9. EndSessionResponse(HeadHash=0x9f3a...) ------------|
```

---

## 4. BACKPRESSURE, FLOW CONTROL & ADAPTIVE SAMPLING

### 4.1. Frame Prioritization Hierarchy
On bandwidth-constrained networks (e.g., 2G/3G rural Indian connections):
1. **Priority 1 (Critical - Never Dropped):** Transcript snippets, Tactic Evidence, Safety Brake interventions, EndSession requests.
2. **Priority 2 (High - Buffered):** 16kHz PCM audio chunks. (Client maintains a circular jitter buffer of 1.5 seconds).
3. **Priority 3 (Supporting - Dynamically Dropped):** Video JPEG frames. If gRPC transmit buffer fills past 75% capacity, video frame sampling drops automatically from 2 fps to 0.5 fps or pauses entirely.

### 4.2. Connection Recovery & Reconnection Invariant
If TCP connection drops during an active call:
1. Client triggers exponential backoff reconnect: $100\text{ms} \rightarrow 300\text{ms} \rightarrow 900\text{ms} \dots$ capped at $3000\text{ms}$.
2. While reconnecting, the **Local Edge Deterministic Guardrail** seamlessly takes over risk evaluation without interrupting the user.
3. Upon gRPC stream re-establishment, client resumes streaming with incremented sequence numbers; session state is synchronized via `session_id`.

---

## 5. HYBRID BACKEND ARCHITECTURE (FastAPI + grpc.aio)

The backend executes a unified hybrid architecture:
- **FastAPI (REST Control Plane):** Handles user authentication, historical session viewing, evidence export, contact management, and dashboard administration.
- **grpc.aio (Real-Time Data Plane):** Manages concurrent, high-throughput bidirectional streams with zero HTTP request overhead.
- Both subsystems share the same database models, memory state, and AI inference engines.
