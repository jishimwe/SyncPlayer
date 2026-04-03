---
type: plan
feature: ci-cd
status: planned
tags:
  - type/plan
  - status/planned
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

**Excluded:**

- Google Play publishing via Fastlane (can be added later when ready for public release)
- Firebase App Distribution (defer until beta testers are onboarded)

## Approach

**GitHub Actions** — free for public repos, generous minutes for private, native Android/Gradle support, already where the repo lives.

Three separate workflow files for clarity and independent triggers:

1. **`ci.yml`** — Runs on every push and PR to any branch. Fast feedback loop: build debug APK, run unit tests, run lint. Uses Gradle caching.

2. **`ci-main.yml`** — Runs on push to `master` only. Everything in `ci.yml` plus: spin up Android emulator (API 34, matches minSdk), run instrumented tests, build release APK (signed with debug key for now), upload APK as artifact.

3. **`release.yml`** — Runs on `v*` tags. Builds signed release AAB/APK using keystore from secrets, creates GitHub Release with the APK attached.

### Key decisions

- **JDK 21**: AGP 9 requires JDK 17+; JDK 21 is the current LTS and what GitHub Actions `setup-java` defaults to.
- **API 34 emulator**: Matches `minSdk = 34`, so instrumented tests run on the minimum supported API.
- **`google-services.json` from secrets**: Base64-encoded in `GOOGLE_SERVICES_JSON` secret, decoded at build time. Without it, the build fails (google-services plugin requirement).
- **Gradle caching**: `actions/cache` on `~/.gradle/caches` and `~/.gradle/wrapper` keyed by `libs.versions.toml` + `*.gradle.kts` hashes.
- **Separate workflows over one monolith**: Easier to reason about, independent failure, different trigger conditions.

## Tasks

### Task 1: PR/push workflow (`ci.yml`)

Create `.github/workflows/ci.yml`:

- Trigger: `push` and `pull_request` on all branches
- Steps:
  1. Checkout code
  2. Set up JDK 21
  3. Decode `google-services.json` from secret into `app/`
  4. Cache Gradle dependencies
  5. Run `./gradlew assembleDebug`
  6. Run `./gradlew test`
  7. Run `./gradlew lint`
  8. Upload lint report as artifact

### Task 2: Main-branch workflow (`ci-main.yml`)

Create `.github/workflows/ci-main.yml`:

- Trigger: `push` to `master` only
- Steps:
  1. Everything from Task 1 (checkout, JDK, secrets, cache, build, test, lint)
  2. Set up Android emulator (API 34, x86_64, system-images;android-34;google_apis;x86_64)
  3. Run `./gradlew connectedAndroidTest`
  4. Build release APK: `./gradlew assembleRelease` (debug-signed for now)
  5. Upload APK as GitHub Actions artifact (90-day retention)

### Task 3: Release workflow (`release.yml`)

Create `.github/workflows/release.yml`:

- Trigger: push of tags matching `v*`
- Steps:
  1. Checkout code
  2. Set up JDK 21
  3. Decode `google-services.json`
  4. Decode release keystore from `RELEASE_KEYSTORE` secret (base64)
  5. Build signed release APK/AAB using keystore credentials from secrets:
     - `RELEASE_KEYSTORE` (base64-encoded `.jks` file)
     - `RELEASE_KEYSTORE_PASSWORD`
     - `RELEASE_KEY_ALIAS`
     - `RELEASE_KEY_PASSWORD`
  6. Create GitHub Release with APK attached using `softprops/action-gh-release`

### Task 4: Update build.gradle.kts for release signing

Add a `signingConfigs` block that reads from environment variables when available, falling back to debug signing for local builds. This lets CI inject real signing credentials without changing local dev workflow.

### Task 5: Documentation

- Update PLAN.md CI/CD section to mark as implemented
- Add secrets setup instructions to this design doc

## Dependencies

No new Gradle dependencies. GitHub Actions are configured via YAML files only.

**Required GitHub Secrets** (must be configured by user in repo Settings > Secrets):

| Secret | Description |
|--------|-------------|
| `GOOGLE_SERVICES_JSON` | Base64-encoded `app/google-services.json` |
| `RELEASE_KEYSTORE` | Base64-encoded release `.jks` keystore file |
| `RELEASE_KEYSTORE_PASSWORD` | Keystore password |
| `RELEASE_KEY_ALIAS` | Key alias within the keystore |
| `RELEASE_KEY_PASSWORD` | Key password |

## Open questions

1. **Release signing**: Do you already have a release keystore, or should the release workflow just use debug signing for now?
2. **Branch protection**: Do you want to enforce CI passing before merges (branch protection rules)?
3. **Instrumented tests on PRs**: Currently only on `master` merge (emulators are slow ~5-10 min). Want them on PRs too?

## Verification

- `ci.yml`: Push a commit or open a PR → workflow runs build + test + lint successfully
- `ci-main.yml`: Push to `master` → workflow runs instrumented tests + uploads APK artifact
- `release.yml`: Push a `v1.0.0` tag → workflow creates GitHub Release with signed APK
- Gradle cache hit on second run (check "Post Cache" step in Actions log)