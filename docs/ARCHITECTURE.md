# RakshaCall Production Architecture

## 1. System Overview

**RakshaCall** is a production-grade, privacy-conscious native Android mobile application designed to serve as an on-device cognitive safety shield against digital-arrest scams, coercive video/voice calls, authority impersonation, and urgent financial demands.

The system is architected as:

```
                  ┌─────────────────────────────────────┐
                  │           PRESENTATION              │
                  │ Jetpack Compose + Material 3 + MVI  │
                  └──────────────────┬──────────────────┘
                                     │ StateFlow / Actions
                  ┌──────────────────▼──────────────────┐
                  │             VIEWMODELS              │
                  │ Home, Protection, Lab, Evidence     │
                  └──────────────────┬──────────────────┘
                                     │ Invocations
                  ┌──────────────────▼──────────────────┐
                  │            USE CASES                │
                  │ ProcessTranscript, AnalyzeRisk...   │
                  └──────────────────┬──────────────────┘
                                     │ Repository Interfaces
                  ┌──────────────────▼──────────────────┐
                  │            REPOSITORIES             │
                  │   User, Session, Risk, Evidence     │
                  └──────────┬──────────────────┬───────┘
                             │                  │
               ┌─────────────▼─────┐      ┌─────▼───────────────┐
               │    LOCAL DATA     │      │     CLOUD / SYNC    │
               │  Room / SQLite    │      │  Offline Sync Queue │
               │  DataStore Prefs  │      │  Firebase / REST    │
               │  Android Keystore │      │  FCM Notifications  │
               └───────────────────┘      └─────────────────────┘
```

## 2. Core Architectural Principles

1. **Local-First Reliability**:
   - The primary conversational safety layer operates 100% on-device.
   - Core risk scoring, scam stage transitions, and Safety Brake triggers never block on network connectivity.
   - Evidence records are hashed and stored locally first.
2. **Strict Clean Architecture**:
   - Presentation layer depends exclusively on ViewModels and UseCases.
   - Domain layer has zero dependencies on Android UI or third-party cloud SDKs.
   - Data layer implements domain interfaces, allowing pluggable storage (Room, Firebase Firestore, REST).
3. **Zero Mock Data Guarantee**:
   - Initial application state is empty.
   - Every displayed risk score, tactic chip, and timeline event is derived from actual running analysis or user input.
4. **Platform Honesty**:
   - Explicitly respects Android application sandboxing.
   - Never claims to silently intercept third-party encrypted calls (WhatsApp, Telegram, Skype).
   - Uses permitted on-device audio streams, the interactive Live Input Lab, and user-initiated capture flows.
