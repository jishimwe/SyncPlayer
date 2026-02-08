# Dependencies & Tech Stack

Complete reference for SyncPlayer's dependencies, versions, and compatibility requirements.

## Version Catalog

All dependencies are managed via Gradle version catalog: `gradle/libs.versions.toml`

**Never hardcode versions in `build.gradle.kts` files.**

## Current Tech Stack

### Core Platform

| Library | Version | Group ID | Notes |
|---------|---------|----------|-------|
| Android Gradle Plugin | 9.0.0 | com.android.tools.build | Bundles Kotlin 2.2.10 |
| Gradle | 9.1.0 | - | Wrapper version |
| Kotlin | 2.2.10 | org.jetbrains.kotlin | Bundled by AGP 9 |
| Kotlin Compose Plugin | 2.2.10 | org.jetbrains.kotlin.plugin.compose | Required alongside bundled Kotlin |

### UI & Compose

| Library | Version | Group ID | Notes |
|---------|---------|----------|-------|
| Compose BOM | 2026.01.01 | androidx.compose | Manages all Compose versions |
| Compose UI | BOM | androidx.compose.ui | |
| Compose Material 3 | BOM | androidx.compose.material3 | Use `PrimaryTabRow` not `TabRow` |
| Compose UI Tooling | BOM | androidx.compose.ui | Debug only |
| Activity Compose | 1.10.0 | androidx.activity | |
| Lifecycle Runtime Compose | 2.9.0 | androidx.lifecycle | For `collectAsStateWithLifecycle()` |

### Dependency Injection

| Library | Version | Group ID | Notes |
|---------|---------|----------|-------|
| Hilt | 2.59 | com.google.dagger | **Must be 2.59+ for AGP 9** |
| Hilt Navigation Compose | 1.3.0 | androidx.hilt | For `hiltViewModel()` |

### Database

| Library | Version | Group ID | Notes |
|---------|---------|----------|-------|
| Room Runtime | 2.7.1 | androidx.room | |
| Room KTX | 2.7.1 | androidx.room | Coroutines support |
| Room Compiler | 2.7.1 | androidx.room | KSP only |

### Annotation Processing

| Library | Version | Group ID | Notes |
|---------|---------|----------|-------|
| KSP | 2.3.5 | com.google.devtools.ksp | **Must be 2.3.4+ for AGP 9** |

### Networking (Future)

| Library | Version | Group ID | Notes |
|---------|---------|----------|-------|
| Retrofit | TBD | com.squareup.retrofit2 | For future sync feature |
| OkHttp | TBD | com.squareup.okhttp3 | |

### Image Loading

| Library | Version | Group ID | Notes |
|---------|---------|----------|-------|
| Coil | 3.1.0 | io.coil-kt.coil3 | **Group changed in v3** |
| Coil Compose | 3.1.0 | io.coil-kt.coil3 | |

### Navigation

| Library | Version | Group ID | Notes |
|---------|---------|----------|-------|
| Navigation Compose | 2.9.0 | androidx.navigation | |

### Testing

| Library | Version | Group ID | Notes |
|---------|---------|----------|-------|
| JUnit 5 Jupiter API | 5.11.4 | org.junit.jupiter | |
| JUnit 5 Jupiter Engine | 5.11.4 | org.junit.jupiter | |
| JUnit Platform Launcher | 1.11.4 | org.junit.platform | Required for JUnit 5 |
| Turbine | 1.2.1 | app.cash.turbine | For testing Flows |
| Kotlin Coroutines Test | 1.10.1 | org.jetbrains.kotlinx | |
| JUnit 4 | 4.13.2 | junit | For instrumented tests only |
| Compose UI Test | BOM | androidx.compose.ui | |
| Compose UI Test Manifest | BOM | androidx.compose.ui | Debug only |

## AGP 9 Compatibility Notes

Android Gradle Plugin 9.0.0 introduces significant changes that affect dependency versions.

### Critical Requirements

**Hilt 2.59+ Required**

AGP 9 changed internal APIs that older Hilt versions depend on.

❌ **Error with Hilt < 2.59:**
```
BaseExtension not found in AGP 9
```

✅ **Solution:**
```toml
[versions]
hilt = "2.59"  # or newer
```

**KSP 2.3.4+ Required**

AGP 9's bundled Kotlin support conflicts with older KSP versions.

❌ **Error with KSP < 2.3.4:**
```
kotlin.sourceSets DSL is not supported in AGP 9
```

✅ **Solution:**
```toml
[versions]
ksp = "2.3.5"  # or newer
```

**KSP Version Format Changed**

Old format: `{kotlinVersion}-{kspVersion}` (e.g., `2.1.0-1.0.29`)
New format: Independent versioning (e.g., `2.3.5`)

✅ **Correct:**
```toml
[versions]
ksp = "2.3.5"
```

❌ **Incorrect:**
```toml
[versions]
ksp = "2.2.10-2.3.5"  # Don't do this
```

### Kotlin Bundling

AGP 9 bundles Kotlin 2.2.10. You **cannot** downgrade Kotlin below this version.

**Still required:**
- `kotlin-compose` plugin (not bundled)

**No longer needed in dependencies:**
- Explicit Kotlin stdlib (bundled)
- Kotlin gradle plugin in project-level dependencies (bundled)

### Compose Compiler

The Compose Compiler is now a Kotlin compiler plugin, not a separate dependency.

**Apply in build.gradle.kts:**
```kotlin
plugins {
    alias(libs.plugins.kotlin.compose)
}
```

**Do NOT add:**
```kotlin
// Don't do this - Compose Compiler is now a plugin
dependencies {
    implementation("androidx.compose.compiler:compiler:...")
}
```

## Library-Specific Gotchas

### Coil 3.x

**Group ID Changed:**
- Old: `io.coil-kt`
- New: `io.coil-kt.coil3`

**API Changes:**
- `AsyncImage` `fallback`/`error` parameters now take `Painter?` (not composable lambdas)
- Use `SubcomposeAsyncImage` if you need composable content in error/fallback/loading slots

**Example:**
```kotlin
// ✅ Correct - using Painter
AsyncImage(
    model = song.albumArtUri,
    error = painterResource(R.drawable.ic_album_placeholder),
    contentDescription = "Album art"
)

// ❌ Incorrect - composable lambda not supported
AsyncImage(
    model = song.albumArtUri,
    error = { Icon(Icons.Default.Album, null) },  // Won't compile
    contentDescription = "Album art"
)

// ✅ Alternative - SubcomposeAsyncImage for composable content
SubcomposeAsyncImage(
    model = song.albumArtUri,
    error = { Icon(Icons.Default.Album, null) },
    contentDescription = "Album art"
)
```

### Material 3

**TabRow Deprecated:**
Use `PrimaryTabRow` instead of `TabRow`

```kotlin
// ✅ Correct
PrimaryTabRow(selectedTabIndex = selectedTab) {
    tabs.forEachIndexed { index, title ->
        Tab(selected = selectedTab == index, onClick = { /*...*/ }) {
            Text(title)
        }
    }
}

// ❌ Deprecated
TabRow(selectedTabIndex = selectedTab) {
    // ...
}
```

### Room

**Schema Export:**

Set `exportSchema = false` unless you need schema versioning:

```kotlin
@Database(
    entities = [SongEntity::class],
    version = 1,
    exportSchema = false  // Set to false for now
)
abstract class SyncPlayerDatabase : RoomDatabase()
```

## JUnit 5 Configuration

JUnit 5 requires additional Gradle configuration.

**In `build.gradle.kts`:**
```kotlin
tasks.withType<Test> {
    useJUnitPlatform()
}

dependencies {
    testImplementation(libs.junit.jupiter.api)
    testRuntimeOnly(libs.junit.jupiter.engine)
    testRuntimeOnly(libs.junit.platform.launcher)  // Required!
}
```

**Why `junit-platform-launcher`?**
Without it, tests won't run and you'll see:
```
No tests found
```

## Adding New Dependencies

### Process

1. **Search for latest stable version** (use web search)
2. **Verify compatibility** with:
   - AGP 9.0.0
   - Kotlin 2.2.10
   - Min SDK 34
3. **Add to version catalog** in `gradle/libs.versions.toml`
4. **Test with** `assembleDebug` before proceeding

### Version Catalog Format

```toml
[versions]
room = "2.7.1"
coil = "3.1.0"

[libraries]
room-runtime = { group = "androidx.room", name = "room-runtime", version.ref = "room" }
room-ktx = { group = "androidx.room", name = "room-ktx", version.ref = "room" }
coil-compose = { group = "io.coil-kt.coil3", name = "coil-compose", version.ref = "coil" }

[plugins]
android-application = { id = "com.android.application", version.ref = "agp" }
kotlin-android = { id = "org.jetbrains.kotlin.android", version.ref = "kotlin" }
kotlin-compose = { id = "org.jetbrains.kotlin.plugin.compose", version.ref = "kotlin" }
ksp = { id = "com.google.devtools.ksp", version.ref = "ksp" }
hilt = { id = "com.google.dagger.hilt.android", version.ref = "hilt" }
```

### Using in build.gradle.kts

```kotlin
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
}

dependencies {
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)
    
    implementation(libs.coil.compose)
}
```

## Updating Dependencies

### When to Update

- Security vulnerabilities
- Bug fixes in current version
- New features needed
- Breaking changes in ecosystem (like AGP updates)

### How to Update

1. **Check release notes** for breaking changes
2. **Update version** in `gradle/libs.versions.toml`
3. **Build**: `gradlew.bat assembleDebug`
4. **Test**: `gradlew.bat test`
5. **Fix any issues** before committing
6. **Update this doc** if compatibility notes change

### Example: Updating Room

```toml
# Before
[versions]
room = "2.7.0"

# After
[versions]
room = "2.7.1"
```

Then run:
```bash
gradlew.bat assembleDebug
gradlew.bat test
```

## Dependency Verification Checklist

Before adding/updating a dependency:

- ✅ Latest stable version confirmed via web search
- ✅ Compatible with AGP 9.0.0
- ✅ Compatible with Kotlin 2.2.10
- ✅ Supports Min SDK 34
- ✅ Added to `libs.versions.toml` (not hardcoded)
- ✅ Correct group ID (especially Coil!)
- ✅ `assembleDebug` succeeds
- ✅ No new warnings in build output

## Common Build Issues

### "BaseExtension not found"

**Cause:** Hilt version < 2.59 with AGP 9
**Fix:** Update Hilt to 2.59+

### "kotlin.sourceSets DSL not supported"

**Cause:** KSP version < 2.3.4 with AGP 9
**Fix:** Update KSP to 2.3.4+

### "No tests found" (JUnit 5)

**Cause:** Missing `junit-platform-launcher` dependency
**Fix:** Add `testRuntimeOnly(libs.junit.platform.launcher)`

### Coil images not loading

**Cause:** Wrong group ID (using old `io.coil-kt` instead of `io.coil-kt.coil3`)
**Fix:** Update group ID to `io.coil-kt.coil3`

### Compose compiler errors

**Cause:** Trying to use old Compose Compiler dependency instead of plugin
**Fix:** Apply `kotlin-compose` plugin, remove Compose Compiler dependency

## Resources

- [AGP 9.0 Release Notes](https://developer.android.com/build/releases/gradle-plugin)
- [Hilt Releases](https://github.com/google/dagger/releases)
- [KSP Releases](https://github.com/google/ksp/releases)
- [Compose BOM Mapping](https://developer.android.com/jetpack/compose/bom/bom-mapping)
- [Coil 3.0 Migration](https://coil-kt.github.io/coil/upgrading_to_coil3/)
