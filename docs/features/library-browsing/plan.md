# Library Browsing Gaps - Plan

## Context

The library browsing feature is implemented and functional, but has 4 gaps documented in `design.md` under "Known Gaps":

1. **Error state is unreachable** — `LibraryViewModel.refreshLibrary()` silently catches all exceptions (line 59), so `LibraryUiState.Error` is defined but never emitted. If a MediaStore scan fails on first launch (empty DB), the user sees an empty list with no explanation.
2. **No user-triggered rescan** — `refreshLibrary()` only runs via `LaunchedEffect(Unit)` on first composition. There's no refresh button or pull-to-refresh gesture, so users can't rescan after adding new music files.
3. **No permission rationale** — `PermissionHandler` shows the same generic "SyncPlayer needs access to your music library" message whether it's the first request or a re-request after denial.
4. **No permanent-denial handling** — If the user selects "Don't ask again", `PermissionHandler` shows the "Grant permission" button which launches the system dialog — but the system dialog no longer appears after permanent denial. The user is stuck.

These gaps make the app frustrating in failure cases: scan errors are invisible, denied permissions have no recovery path, and users can't manually refresh.

## Scope

**Included:**
- Making `LibraryUiState.Error` reachable when a scan fails and the DB is empty
- Adding a refresh button in the top app bar
- Adding automatic refresh on app creation and on app resume (if last scan was >24 hours ago)
- Adding an `isRefreshing` loading indicator
- Distinguishing three permission states: first request, rationale, permanent denial
- Adding "Open settings" button for permanently denied permission
- Updating the design doc to reflect changes

**Excluded:**
- Changing the scan strategy (still full deleteAll + insertAll)
- Adding search, filtering, or sorting
- Changing the tab structure or navigation

## Approach

**Error handling strategy: stale data over error screen.** If a scan fails but the DB already has songs from a previous scan, show the stale data rather than an error screen. The error state should only appear when the scan fails AND the DB is empty (no data to show). This is better than always showing an error because stale data is more useful to the user than an error message — they can still browse and play their library while the issue resolves itself.

**Refresh: toolbar button + automatic lifecycle-based refresh.** The toolbar button provides manual control. Automatic refresh happens on app creation (fresh start) and on app resume if the last scan was more than 24 hours ago. This approach avoids unnecessary rescans (local data is stable) while still catching newly added music files when the user returns to the app after a long period. Pull-to-refresh is NOT used because this is local data, not network data — the data doesn't change unless the user adds files, and they know when that happens.

**Permission states via `rememberSaveable` + `shouldShowRequestPermissionRationale()`.** Track whether we've already launched the permission request with a `rememberSaveable` boolean. After a denial:
- If `shouldShowRequestPermissionRationale()` returns `true` → user denied once, show rationale explaining why the permission matters
- If it returns `false` → user permanently denied, show "Open settings" button that launches the app's system settings page via `Settings.ACTION_APPLICATION_DETAILS_SETTINGS`

This approach avoids adding any new dependencies — `shouldShowRequestPermissionRationale()` is a standard Activity API, and `Settings.ACTION_APPLICATION_DETAILS_SETTINGS` is a standard Android intent.

## Tasks

### Task 1: Make error state reachable in ViewModel

**What changes:** In `LibraryViewModel.kt`:
- Add `_refreshError: MutableStateFlow<String?>` (starts null)
- Include it in the `combine` that produces `uiState`: if `_refreshError` is non-null AND `songs` is empty, emit `Error`; otherwise emit `Loaded`
- In `refreshLibrary()`: clear error on success, set error message on catch

**Why:** The `Error` variant exists in `LibraryUiState` and the UI already renders it (LibraryScreen.kt line 88-100), but the ViewModel never emits it. By folding the error into `combine`, the state reacts correctly: error only shows when there's genuinely nothing to display.

**Why not always show error?** If the user has 500 songs cached and a scan fails, showing an error screen hides their entire library. Stale data is more useful.

**Tests to add** in `LibraryViewModelTest.kt`:
- `refreshLibrary error with empty DB emits Error state`
- `refreshLibrary error with existing songs keeps Loaded state`

### Task 2: Add refresh button, lifecycle-based refresh, and isRefreshing state

**What changes:**
- In `LibraryViewModel.kt`:
    - Add `_isRefreshing: MutableStateFlow<Boolean>`, set `true` before scan, `false` after (in finally block)
    - Add `_lastScanTimestamp: MutableStateFlow<Long>` initialized to `0L`
    - Add `onAppResumed()` function that checks if `System.currentTimeMillis() - lastScanTimestamp > 24 hours`, and if so, calls `refreshLibrary()`
    - Update `refreshLibrary()` to set `_lastScanTimestamp` to current time at the start of the scan
    - `init` block already calls `refreshLibrary()` on creation, so no change needed there
- In `LibraryScreen.kt`:
    - Add `isRefreshing: Boolean` param to `LibraryScreenContent`
    - Rename `onRetry` → `onRefresh` for consistency
    - Add `IconButton(Icons.Default.Refresh)` in `TopAppBar` actions
    - Add lifecycle observer using `DisposableEffect` that calls `viewModel.onAppResumed()` on `Lifecycle.Event.ON_RESUME`
- In `LibraryScreen` (ViewModel-connected version): collect and pass `isRefreshing`
- Update `LibraryScreenTest.kt`: all `LibraryScreenContent` calls need the new `isRefreshing` param and renamed `onRefresh`

**Why rename onRetry → onRefresh?** The callback now serves two purposes (retry after error AND manual refresh), so `onRefresh` is more accurate. The Error state's button label stays "Retry" — only the callback name changes.

**Why a toolbar button?** It's always visible regardless of tab or list state, making it discoverable. It provides manual control when the user knows they've added music.

**Why 24 hours?** Local music files don't change frequently. Most users add music rarely (weekly or monthly), so checking once per day on app resume is sufficient to catch new files without wasteful scanning. The manual button covers immediate needs.

**Why lifecycle observer?** The `ON_RESUME` event fires when the app returns to the foreground from background. By checking the timestamp here, we catch the case where the user added music while the app was backgrounded and returns after 24+ hours.

**Test to add** in `LibraryViewModelTest.kt`:
- `isRefreshing is true during refresh and false after`
- `onAppResumed within 24 hours does not trigger refresh`
- `onAppResumed after 24 hours triggers refresh`

### Task 3: Improve permission handling

**What changes:** Rewrite `PermissionHandler.kt` to handle three states:

1. **Not yet requested** (first launch): Auto-launch the system dialog via `LaunchedEffect`. This is the current behavior and stays the same.
2. **Denied once** (`shouldShowRequestPermissionRationale()` returns `true`): Show an explanation of why the permission is needed ("SyncPlayer scans your device for music files...") with a "Grant permission" button.
3. **Permanently denied** (`shouldShowRequestPermissionRationale()` returns `false` after a request): Show a message explaining the permission must be granted in settings, with an "Open settings" button that launches `Settings.ACTION_APPLICATION_DETAILS_SETTINGS`.

**Implementation approach:**
- Track `hasRequested` via `rememberSaveable` — starts `false`, set to `true` after the launcher fires
- In the launcher callback, if `granted` is false, check `shouldShowRequestPermissionRationale()` (need the Activity context) to distinguish denial type
- Store a `permissionState` enum: `NotRequested`, `Rationale`, `PermanentlyDenied`

**Why `rememberSaveable`?** Survives configuration changes (rotation). Without tracking whether we've already requested, we can't distinguish "not yet asked" from "permanently denied" — both have `shouldShowRequestPermissionRationale()` returning `false`.

**Why `Settings.ACTION_APPLICATION_DETAILS_SETTINGS`?** It's the standard Android way to deep-link to the app's settings page where the user can manually grant permissions. No new dependencies needed.

### Task 4: Update design doc

**What changes:** Update `docs/features/library-browsing/design.md`:
- Update descriptions for `LibraryViewModel.kt` (error flow, isRefreshing, lastScanTimestamp, onAppResumed)
- Update description for `LibraryScreen.kt` (refresh button, lifecycle observer for auto-refresh)
- Update description for `PermissionHandler.kt` (three-state handling)
- Add design decisions for error strategy, lifecycle-based refresh (why 24 hours, why not pull-to-refresh), and permission handling
- Remove "Known Gaps" section (all gaps addressed)

## Dependencies

None. Everything needed is already in the project:
- `shouldShowRequestPermissionRationale()` is a standard Activity API
- `Settings.ACTION_APPLICATION_DETAILS_SETTINGS` is a standard Android intent
- `Icons.Default.Refresh` is in Material Icons (already imported via `material-icons-extended`)
- `Lifecycle.Event.ON_RESUME` and `LifecycleEventObserver` are part of AndroidX Lifecycle (already in project)

## Open questions

None — the approach uses standard Android patterns with no ambiguity.

## Verification

**After each task:**
- `gradlew.bat assembleDebug` succeeds

**After Tasks 1-2:**
- `gradlew.bat test` passes (including new tests)
- Manual test: Add music file to device, wait 24+ hours (or change timestamp in code for testing), resume app → verify scan happens
- Manual test: Resume app multiple times within 24 hours → verify scan doesn't happen each time
- Manual test: Tap refresh button → verify scan happens immediately regardless of timestamp

**After Task 3:**
- Manual test: deny permission once → see rationale screen
- Manual test: deny permanently → see "Open settings" button
- Manual test: grant via settings → app loads library

**After Task 4:**
- Review design doc for completeness
