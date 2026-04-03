---
type: plan
feature: sync
phase: 6
status: complete
tags:
  - type/plan
  - status/complete
  - feature/sync
---

# Fix Firestore Sync Not Writing Data — Plan

## Context

Sync infrastructure (Phase 6) was implemented: `SyncRepositoryImpl`, `SyncOrchestrator`, `AuthRepositoryImpl`, Firestore data models, conflict resolution, and Settings UI. However, nothing appears in Firebase — no collections, no documents. The sync chain runs silently without producing data in Firestore.

## Scope

**Included:**
- Diagnose why Firestore writes produce no visible data
- Add logging to surface hidden errors
- Fix identified root causes
- Verify data appears in Firebase Console

**Excluded:**
- New sync features (background sync, WorkManager)
- Conflict resolution changes
- UI changes beyond existing Settings screen

## Approach

The code in `SyncRepositoryImpl.kt` is structurally correct — Firestore API calls are properly formed. The problem is upstream. There are 5 likely failure points, investigated in priority order:

### 1. Silent error swallowing (highest priority)
`SyncOrchestrator.sync()` catches all exceptions and emits `SyncStatus.Error(message)` but has **zero logging**. If Firestore rejects writes (auth rules, missing DB), we'd never see it in Logcat. Adding `Log.e` will immediately reveal the actual error.

### 2. Firestore security rules
No `firestore.rules` file exists in the project. Default Firestore **production mode** rules deny all reads/writes. Every `.set().await()` would throw a `FirebaseFirestoreException` with `PERMISSION_DENIED`, caught silently by the orchestrator.

### 3. Firestore database not provisioned
`google-services.json` exists for project `syncplayer-66d79`, but a Firestore database may not have been created in the Firebase Console. The SDK initializes fine without a database — it only fails on read/write.

### 4. Push filter: `lastModified` may be 0
Songs are pushed only when `it.lastModified > lastSync` (line 83 of `SyncOrchestrator`). `lastSyncTime` starts at `0`. If songs were inserted before the `lastModified` column existed (pre-migration-4→5), their `lastModified` is `0`. `0 > 0` is `false` — nothing gets pushed.

### 5. Auth/userId is null
If Google Sign-In didn't complete, `currentUserId` returns `null` and `sync()` returns immediately at line 57. No error is emitted.

## Tasks

### Task 1: Add diagnostic logging to SyncOrchestrator
**File:** `data/sync/SyncOrchestrator.kt`
- Add `Log.d` at entry of `sync()` with userId
- Add `Log.d` before push/pull with counts of records being synced
- Add `Log.e` in the catch block with full stack trace
- Add `Log.d` after successful sync with timestamp

### Task 2: Add logging to SyncRepositoryImpl
**File:** `data/sync/SyncRepositoryImpl.kt`
- Add `Log.d` in each push method showing document path and data size
- Add `Log.d` in each pull method showing document count returned

### Task 3: Add logging to AuthRepositoryImpl
**File:** `data/sync/AuthRepositoryImpl.kt`
- Add `Log.d` on auth state changes (signed in/out)
- Add `Log.d` in `signInWithToken` success/failure

### Task 4: Verify Firestore Console setup (user action)
- Confirm Firestore database is created in Firebase Console for project `syncplayer-66d79`
- Confirm security rules allow authenticated writes to `users/{userId}/**`
- Recommended rules:
  ```
  rules_version = '2';
  service cloud.firestore {
    match /databases/{database}/documents {
      match /users/{userId}/{document=**} {
        allow read, write: if request.auth != null && request.auth.uid == userId;
      }
    }
  }
  ```

### Task 5: Fix `lastModified = 0` push filter gap
**File:** `data/sync/SyncOrchestrator.kt`
- In `push()`, change the filter from `it.lastModified > lastSync` to `it.lastModified >= lastSync` for first sync (when `lastSync == 0`), OR treat `lastModified == 0` as "never synced, always push"
- Recommended: `allSongs.filter { it.lastModified > lastSync || (lastSync == 0L && it.lastModified == 0L) }`
- Same treatment for playlists filter

### Task 6: Build and test
- Run `assembleDebug` after each task
- Run `test` to ensure existing tests pass
- Manual verification: sign in, trigger sync, check Logcat for diagnostic output, check Firebase Console for documents

## Open Questions

1. **Is the Firestore database created?** — Need user to confirm in Firebase Console
2. **What are the current security rules?** — Need user to check in Firebase Console → Firestore → Rules
3. **Is the user successfully signed in?** — Does Settings screen show the signed-in state with name/email?

## Verification

- `assembleDebug` succeeds after each task
- `test` passes all tests
- Logcat shows sync diagnostic messages when "Sync now" is tapped
- If auth/rules are correct: documents appear in Firebase Console under `users/{uid}/songs/`
- If auth/rules are wrong: Logcat shows the specific error (PERMISSION_DENIED, NOT_FOUND, etc.)