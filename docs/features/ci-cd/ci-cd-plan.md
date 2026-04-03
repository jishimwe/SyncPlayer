---
type: plan
feature: ci-cd
status: implemented
tags:
  - type/plan
  - status/implemented
  - feature/ci-cd
---

# CI/CD Pipeline - Plan

## Context

All verification is currently manual (`gradlew assembleDebug`, `gradlew test`). The project has 110 JVM unit tests and 78 instrumented tests. As development continues toward Phase 8, automated builds and tests on every push will catch regressions early and remove manual toil. The PLAN.md CI/CD section defines three tiers: PR checks, merge-to-main checks, and tag-triggered releases.

## Scope

**Included:**

- Workflow 1: PR / push checks (build, unit tests, lint)
- Workflow 2: Merge-to-main checks (instrumented tests on emulator, release APK build, artifact upload)
- Workflow 3: Tag-triggered release (signed release AAB, GitHub Release with APK)
- Gradle caching for fast CI runs
- Secrets management for keystore and `google-services.json`
- Gradle wrapper validation (supply-chain safety)
- Concurrency controls to cancel stale runs
- Test result artifact uploads for debugging CI failures

**Excluded:**

- Google Play publishing via Fastlane (can be added later when ready for public release)
- Firebase App Distribution (defer until beta testers are onboarded)

## Approach

**GitHub Actions** — free for public repos, generous minutes for private, native Android/Gradle support, already where the repo lives.

All workflows run on **`ubuntu-latest`** (required for Android SDK tooling and KVM-accelerated emulators).

Three separate workflow files for clarity and independent triggers, with shared steps extracted into a **reusable workflow** to avoid drift:

1. **`ci-shared.yml`** — Reusable workflow (`workflow_call`). Contains the common steps: checkout, Gradle wrapper validation, JDK setup, secrets decoding, Gradle caching, build, unit tests, lint. Called by both `ci.yml` and `ci-main.yml`.

2. **`ci.yml`** — Runs on every push and PR to any branch. Calls `ci-shared.yml`. Fast feedback loop.

3. **`ci-main.yml`** — Runs on push to `master` only. Calls `ci-shared.yml`, then adds: emulator-based instrumented tests, release APK build, artifact upload.

4. **`release.yml`** — Runs on `v*` tags. Builds signed release AAB/APK using keystore from secrets, creates GitHub Release with the APK attached.

### Key decisions

- **`ubuntu-latest` runner**: Required for Android SDK. Linux runners support KVM hardware acceleration for fast emulator boot.
- **JDK 21**: AGP 9 requires JDK 17+; JDK 21 is the current LTS and what GitHub Actions `setup-java` defaults to.
- **API 34 emulator via `reactivecircus/android-emulator-runner@v2`**: Mature action that handles emulator boot, KVM acceleration, and timeout. Matches `minSdk = 34`.
- **`google-services.json` from secrets**: Base64-encoded in `GOOGLE_SERVICES_JSON` secret, decoded at build time. Without it, the build fails (google-services plugin requirement).
- **Gradle wrapper validation**: `gradle/wrapper-validation-action@v3` before any `./gradlew` call to detect tampered wrapper JARs (supply-chain protection).
- **Gradle caching**: `actions/cache` on `~/.gradle/caches` and `~/.gradle/wrapper` keyed by `libs.versions.toml` + `*.gradle.kts` hashes.
- **Concurrency with cancel-in-progress**: Each workflow uses `concurrency: { group: ${{ github.workflow }}-${{ github.ref }}, cancel-in-progress: true }` to kill stale runs on rapid pushes.
- **Job timeouts**: All jobs get `timeout-minutes` — 15 for build-only, 30 for emulator jobs — to prevent hung runners from burning minutes.
- **Reusable workflow over duplication**: `ci.yml` and `ci-main.yml` share ~80% of their steps. A `workflow_call` reusable workflow avoids copy-paste drift.

### Getting the APK to your phone

The release pipeline provides two delivery paths:

1. **Per-commit builds (automatic)** — Every push to `master` via `ci-main.yml` builds a release APK and uploads it as a GitHub Actions artifact (90-day retention). Navigate to the Actions tab → latest run → download `release-apk` artifact. Requires GitHub login. Best for testing in-progress work.

2. **Tagged releases (manual trigger)** — When you push a `v*` tag, `release.yml` builds a signed APK and attaches it to a GitHub Release. The release page has a direct download link — no login needed if the repo is public. On your phone: open the release URL in a browser → tap the `.apk` asset → install. Best for milestone builds you want to keep.

**Recommended personal workflow:**
```
# You're happy with master, want it on your phone:
git tag v0.1.0
git push origin v0.1.0
# → GitHub Release created with signed APK
# → Open github.com/<user>/SyncPlayer/releases/latest on phone → download → install
```

Since the repo is private, both delivery paths require authentication. Use the **GitHub mobile app** to browse Actions artifacts or Release assets and download APKs directly. If that becomes friction, Firebase App Distribution can push install links via email/notification — but that's deferred for now.

## Tasks

### Task 1: Reusable shared workflow (`ci-shared.yml`)

Create `.github/workflows/ci-shared.yml`:

- Trigger: `workflow_call` (called by other workflows, not triggered directly)
- Inputs: none (all secrets passed via `secrets: inherit`)
- Steps:
  1. Checkout code
  2. Validate Gradle wrapper (`gradle/wrapper-validation-action@v3`)
  3. Set up JDK 21 (`actions/setup-java@v4`, distribution: `temurin`)
  4. Decode `google-services.json` from secret into `app/`
  5. Cache Gradle dependencies (`actions/cache@v4`, key includes `libs.versions.toml` + `*.gradle.kts` hashes)
  6. Run `./gradlew assembleDebug`
  7. Run `./gradlew test`
  8. Upload JUnit test results (`actions/upload-artifact@v4`, path: `app/build/test-results/`)
  9. Run `./gradlew lintDebug`
  10. Upload lint report as artifact

### Task 2: PR/push workflow (`ci.yml`)

Create `.github/workflows/ci.yml`:

- Trigger: `push` and `pull_request` on all branches
- Concurrency: `group: ci-${{ github.ref }}`, `cancel-in-progress: true`
- Job: calls `ci-shared.yml` via `uses: ./.github/workflows/ci-shared.yml`
- `timeout-minutes: 15`

### Task 3: Main-branch workflow (`ci-main.yml`)

Create `.github/workflows/ci-main.yml`:

- Trigger: `push` to `master` only
- Concurrency: `group: ci-main-${{ github.ref }}`, `cancel-in-progress: true`
- Job 1: calls `ci-shared.yml` (build + unit tests + lint)
- Job 2 (depends on Job 1): instrumented tests + release APK
  - `timeout-minutes: 30`
  - Enable KVM acceleration (`/dev/kvm` permissions)
  - Run instrumented tests via `reactivecircus/android-emulator-runner@v2`:
    - `api-level: 34`, `arch: x86_64`, `target: google_apis`
    - `script: ./gradlew connectedAndroidTest`
  - Upload instrumented test results as artifact
  - Build release APK: `./gradlew assembleRelease` (debug-signed for now)
  - Upload APK as GitHub Actions artifact (90-day retention)

### Task 4: Release workflow (`release.yml`)

Create `.github/workflows/release.yml`:

- Trigger: push of tags matching `v*`
- `timeout-minutes: 20`
- Steps:
  1. Checkout code
  2. Validate Gradle wrapper
  3. Set up JDK 21
  4. Decode `google-services.json`
  5. Decode release keystore from `RELEASE_KEYSTORE` secret (base64) into a temp file
  6. Build signed release APK/AAB using keystore credentials from env vars:
     - `RELEASE_KEYSTORE` (base64-encoded `.jks` file)
     - `RELEASE_KEYSTORE_PASSWORD`
     - `RELEASE_KEY_ALIAS`
     - `RELEASE_KEY_PASSWORD`
  7. Create GitHub Release with APK attached using `softprops/action-gh-release@v2`
  8. Clean up decoded keystore file (security hygiene, even though runner is ephemeral)

### Task 5: Update build.gradle.kts for release signing

Add a `signingConfigs` block that reads from environment variables when available, falling back to debug signing for local builds. This lets CI inject real signing credentials without changing local dev workflow.

```kotlin
signingConfigs {
    create("release") {
        val ks = System.getenv("RELEASE_KEYSTORE_PATH")
        if (ks != null) {
            storeFile = file(ks)
            storePassword = System.getenv("RELEASE_KEYSTORE_PASSWORD")
            keyAlias = System.getenv("RELEASE_KEY_ALIAS")
            keyPassword = System.getenv("RELEASE_KEY_PASSWORD")
        }
    }
}
```

### Task 6: Documentation

- Update PLAN.md CI/CD section to mark as implemented
- Add secrets setup instructions to this design doc
- Document the tag-and-release workflow for getting APKs to device

## Dependencies

No new Gradle dependencies. GitHub Actions are configured via YAML files only.

**Required GitHub Secrets** (must be configured by user in repo Settings > Secrets):

| Secret                      | Description                                 |
| --------------------------- | ------------------------------------------- |
| `GOOGLE_SERVICES_JSON`      | Base64-encoded `app/google-services.json`   |
| `RELEASE_KEYSTORE`          | Base64-encoded release `.jks` keystore file |
| `RELEASE_KEYSTORE_PASSWORD` | Keystore password                           |
| `RELEASE_KEY_ALIAS`         | Key alias within the keystore               |
| `RELEASE_KEY_PASSWORD`      | Key password                                |

## Open questions

1. **Release signing**: Do you already have a release keystore, or should the release workflow just use debug signing for now?
2. **Branch protection**: Do you want to enforce CI passing before merges (branch protection rules)?
3. **Instrumented tests on PRs**: Currently only on `master` merge (emulators are slow ~5-10 min). Want them on PRs too?
4. ~~**Repo visibility**~~: Private repo — APK downloads via GitHub mobile app (resolved).

## Verification

- `ci-shared.yml`: Reusable workflow validates, builds, tests, lints, uploads reports
- `ci.yml`: Push a commit or open a PR → workflow runs build + test + lint successfully
- `ci-main.yml`: Push to `master` → workflow runs instrumented tests + uploads APK artifact
- `release.yml`: Push a `v1.0.0` tag → workflow creates GitHub Release with signed APK
- Gradle cache hit on second run (check "Post Cache" step in Actions log)
- Concurrency: push two commits quickly → first run is cancelled, only second completes
- Timeout: verify hung emulator job is killed after 30 minutes (not left running)
- **End-to-end APK install**: Download APK from GitHub Release on phone → install → app launches