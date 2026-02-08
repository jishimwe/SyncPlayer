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
- Adding pull-to-refresh on list/grid content
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

**Refresh: toolbar button + pull-to-refresh.** Both patterns are standard Android UX. The toolbar button is always visible and discoverable. Pull-to-refresh is the expected gesture on scrollable lists. Empty states only get the toolbar button (nothing to pull on).

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

### Task 2: Add refresh button and isRefreshing state

**What changes:**
- In `LibraryViewModel.kt`: add `_isRefreshing: MutableStateFlow<Boolean>`, set `true` before scan, `false` after (in finally block)
- In `LibraryScreen.kt`: add `isRefreshing: Boolean` param to `LibraryScreenContent`, rename `onRetry` → `onRefresh` for consistency, add `IconButton(Icons.Default.Refresh)` in `TopAppBar` actions
- In `LibraryScreen` (ViewModel-connected version): collect and pass `isRefreshing`
- Update `LibraryScreenTest.kt`: all `LibraryScreenContent` calls need the new `isRefreshing` param and renamed `onRefresh`

**Why rename onRetry → onRefresh?** The callback now serves two purposes (retry after error AND manual refresh), so `onRefresh` is more accurate. The Error state's button label stays "Retry" — only the callback name changes.

**Why a toolbar button?** It's always visible regardless of tab or list state, making it discoverable. It also serves as the refresh mechanism for empty states where pull-to-refresh isn't available.

**Test to add** in `LibraryViewModelTest.kt`:
- `isRefreshing is true during refresh and false after`

### Task 3: Add pull-to-refresh to tab content

**What changes:** In `LibraryScreen.kt`:
- Wrap `SongsTab`, `AlbumsTab`, and `ArtistsTab` list content (the `LazyColumn`/`LazyVerticalGrid` — NOT the empty states) in Material 3 `PullToRefreshBox`
- Pass `isRefreshing` and `onRefresh` down to tab composables
- Add `@OptIn(ExperimentalMaterial3Api::class)` where needed

**Why `PullToRefreshBox`?** It's the Material 3 pull-to-refresh component, available in our Compose BOM (2026.01.01). It wraps scrollable content and shows a refresh indicator when pulled.

**Why not on empty states?** There's nothing to pull on when the list is empty. The toolbar refresh button covers this case.

### Task 4: Improve permission handling

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

### Task 5: Update design doc

**What changes:** Update `docs/features/library-browsing/design.md`:
- Update descriptions for `LibraryViewModel.kt` (error flow, isRefreshing)
- Update description for `LibraryScreen.kt` (refresh button, pull-to-refresh)
- Update description for `PermissionHandler.kt` (three-state handling)
- Add design decisions for error strategy and permission handling
- Remove "Known Gaps" section (all gaps addressed)

## Dependencies

None. Everything needed is already in the project:
- `PullToRefreshBox` is in Material 3 (Compose BOM 2026.01.01)
- `shouldShowRequestPermissionRationale()` is a standard Activity API
- `Settings.ACTION_APPLICATION_DETAILS_SETTINGS` is a standard Android intent
- `Icons.Default.Refresh` is in Material Icons (already imported via `material-icons-extended`)

## Open questions

None — the approach uses standard Android patterns with no ambiguity.

## Verification

**After each task:**
- `gradlew.bat assembleDebug` succeeds

**After Tasks 1-2:**
- `gradlew.bat test` passes (including new tests)

**After Task 3:**
- `gradlew.bat assembleDebug` succeeds (pull-to-refresh is gesture-based, hard to unit test)

**After Task 4:**
- Manual test: deny permission once → see rationale screen
- Manual test: deny permanently → see "Open settings" button
- Manual test: grant via settings → app loads library

**After Task 5:**
- Review design doc for completeness
