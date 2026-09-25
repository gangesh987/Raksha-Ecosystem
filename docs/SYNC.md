# RakshaCall Offline-First Synchronization Engine

## 1. Synchronization Architecture

RakshaCall implements a strict offline-first synchronization model. Local SQLite storage is the authoritative source of truth.

```
┌──────────────────────────────────────────────────────────────┐
│                    LOCAL ROOM / SQLITE                       │
│    Active Session ➔ Detect Tactic ➔ Append SHA-256 Block     │
└──────────────────────────────┬───────────────────────────────┘
                               │
                               ▼
┌──────────────────────────────────────────────────────────────┐
│                    OFFLINE SYNC QUEUE                        │
│   SyncState: LOCAL_ONLY ➔ PENDING ➔ SYNCING ➔ SYNCED         │
└──────────────────────────────┬───────────────────────────────┘
                               │ Online & Cloud Enabled
                               ▼
┌──────────────────────────────────────────────────────────────┐
│                  CLOUD SYNC (REST / FIREBASE)                │
│    POST /v1/sync OR Cloud Firestore Document Commit          │
│    Acknowledgment returns ➔ Updates syncState to SYNCED      │
└──────────────────────────────────────────────────────────────┘
```

## 2. Sync States & Conflict Resolution

| State | Description | Transition Trigger |
| :--- | :--- | :--- |
| `LOCAL_ONLY` | Stored exclusively on device. Cloud sync disabled. | Default for offline-only mode. |
| `PENDING` | Queued for remote sync. | Added when cloud sync is toggled ON. |
| `SYNCING` | Network transmission currently in flight. | `SyncManager.triggerSync()` running. |
| `SYNCED` | Server has acknowledged receipt with timestamp. | HTTP 200 OK / Firestore write success. |
| `FAILED` | Transmission failed (timeout/no network). | Kept in queue for subsequent retry. |
| `CONFLICT` | Divergent hash or timestamp detected. | Local device evidence takes precedence. |

## 3. Resilience Guarantees
1. **Zero Evidence Drop**: If the network disconnects mid-call, local protection and hash chaining continue with 100% functionality.
2. **Exponential Backoff**: Failed sync calls retry after 2s, 4s, 8s, up to 60s, without waking unnecessary background wake locks.
3. **No Lock-in**: Cloud sync can be enabled or disabled at any time in Settings without data loss.
