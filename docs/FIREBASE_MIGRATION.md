# RakshaCall Firebase Cloud Migration Guide

## 1. Architecture Readiness

RakshaCall is designed with Clean Architecture repository interfaces (`UserRepository`, `SessionRepository`, `EvidenceRepository`, `TrustedContactRepository`). Switching from 100% offline local Room to Firebase Cloud requires **zero modifications to UI screens or domain business logic**.

```
                   Domain Repository Interface
                                │
                 ┌──────────────┴──────────────┐
                 ▼                             ▼
        LocalUserRepository          FirebaseAuthRepository
        (Room + DataStore)             (Firebase Auth)
```

## 2. Firebase Services Configuration

1. **Firebase Authentication**:
   - Enable Phone Authentication and Google Sign-In in Firebase Console.
   - Provider-neutral user repository: `FirebaseAuthRepository` maps Firebase UID to `User` domain model.
2. **Cloud Firestore Schema**:
   - Collections:
     - `users/{uid}`
     - `sessions/{sessionId}`
     - `sessions/{sessionId}/evidence/{eventId}`
     - `trustedContacts/{uid}/contacts/{contactId}`
3. **Firestore Security Rules**:
   ```javascript
   rules_version = '2';
   service cloud.firestore {
     match /databases/{database}/documents {
       match /users/{userId} {
         allow read, write: if request.auth != null && request.auth.uid == userId;
       }
       match /sessions/{sessionId} {
         allow read, write: if request.auth != null && request.auth.uid == resource.data.userId;
       }
       match /sessions/{sessionId}/evidence/{eventId} {
         allow read, create: if request.auth != null;
         allow update, delete: if false; // Append-only evidence immutability
       }
     }
   }
   ```
4. **Firebase Cloud Messaging (FCM)**:
   - Push notifications handled by `FirebaseNotificationHandler.kt`.
   - Never exposes sensitive transcripts or OTPs on lock-screen banners.
