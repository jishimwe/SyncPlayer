# Implementation Process

This document outlines the step-by-step workflow for implementing features in SyncPlayer.

## When to Stop and Ask

Before starting any work, stop and ask for clarification if:

- The user's request is ambiguous or could be interpreted multiple ways
- Multiple valid approaches exist (present options with trade-offs)
- The design decision will significantly affect future features
- You're unsure about scope (what's included vs. what should be separate)

## Feature Implementation Workflow

### Step 1: Confirm Scope

If the request is ambiguous, ask clarifying questions before writing any plan or code.

**Example clarifying questions:**
- "Should this include X, or is that a separate feature?"
- "Do you want approach A (simpler, less flexible) or B (more complex, more extensible)?"
- "Should this handle edge case Y, or is that out of scope?"

### Step 2: Write Plan Doc

For any new feature or significant change:

1. **Read existing code first** to understand current structure
2. **Create** `docs/features/<feature-name>/plan.md` following the required sections:
   - Context
   - Scope
   - Approach
   - Tasks
   - Dependencies (delete if none)
   - Open questions (delete if none)
   - Verification
3. **Verify dependency versions** (only for NEW or UPDATED dependencies):
   - Search the web for latest stable version
   - Verify compatibility with AGP 9 / Kotlin 2.2.10
   - Check correct group/artifact names
   - For existing dependencies in `libs.versions.toml`, assume they're current
4. **Present the plan** and **WAIT for explicit approval**
   - Approval signals: "approved", "proceed", "looks good", "go ahead"
   - If changes requested, revise the plan before proceeding
   - Do NOT start coding until explicitly approved

### Step 3: Implement Incrementally

After receiving approval, implement in small, compilable layers. **Never write all files before the first build.**

#### Layer 1: Dependencies and Build Config

- Add new dependencies to `gradle/libs.versions.toml`
- Update `build.gradle.kts` files
- Run `assembleDebug` and fix any build errors
- Briefly explain what was added and why

**Example:**
```
I'm adding Room dependency version 2.7.1 to the version catalog because
we need a local database to store songs. This requires adding both the
runtime and KSP compiler dependencies.
```

#### Layer 2: Models and Data Layer

- Add or modify data classes in `model/`
- Add or modify DAOs, database entities in `data/local/`
- Add or modify repositories in `data/`
- Run `assembleDebug` and fix any build errors
- Explain the data structure and flow

**Example:**
```
Creating the Song entity with fields for title, artist, duration, and filePath.
The SongDao provides methods to insert, query, and delete songs. The repository
will coordinate between the DAO and MediaStore scanner.
```

#### Layer 3: Dependency Injection and ViewModel

- Add DI modules in `di/` if needed
- Create or modify ViewModel with state and event handling
- Run `assembleDebug` and fix any build errors
- Explain the state management approach

**Example:**
```
The LibraryViewModel exposes a StateFlow<LibraryUiState> that the screen observes.
It uses a sealed interface for state (Loading, Success, Error) and handles events
like song selection and filtering through the onEvent function.
```

#### Layer 4: UI Composables

- Create screen composable with ViewModel
- Create content composable for testing
- Create any supporting composables
- Wire up navigation if needed
- Run `assembleDebug` and fix any build errors
- Explain the component hierarchy and user flow

**Example:**
```
LibraryScreen obtains the ViewModel via Hilt and collects state. It delegates
rendering to LibraryScreenContent, which displays a LazyColumn of SongListItems.
Each item shows album art, title, and artist using our reusable SongCard component.
```

#### Layer 5: Tests

- Add unit tests for ViewModel and repository
- Add UI tests if critical user flow
- Run `test` and fix any failures
- Explain test coverage

**Example:**
```
LibraryViewModelTest covers state transitions: initial loading state, successful
song load, and error handling. We use Turbine to test StateFlow emissions and
verify that events trigger the correct state changes.
```

### Step 4: Write Design Doc

After implementation is complete and builds successfully:

1. Create or update `docs/features/<feature-name>/design.md`
2. Follow the required sections:
   - Overview
   - What was built
   - Design decisions
   - Known gaps (delete if none)
3. Document what was **actually** built (may differ from plan)
4. Include key design decisions and trade-offs

### Step 5: Iterate if Needed

If changes are requested after initial implementation:

1. Make the requested changes
2. Re-test with `assembleDebug` and `test`
3. Update design doc if decisions changed

## Code Development Guidelines

### Before Writing Code

- **Read existing files** to understand current structure
- **Follow existing patterns** (MVVM, naming conventions, file organization)
- **Plan minimal changes** focused on what was requested

### While Writing Code

- **Explain before each layer**: Briefly describe what and why
- **Build incrementally**: Don't skip the `assembleDebug` steps
- **Fix errors immediately**: Don't accumulate build failures
- **Stay focused**: No drive-by refactors or style changes

### Dependency Management

- **Never hardcode versions** in `build.gradle.kts`
- All dependencies go in `gradle/libs.versions.toml`
- When adding a **NEW** dependency:
  - Search web for latest stable version
  - Verify compatibility with AGP 9 / Kotlin 2.2.10
  - Check correct group/artifact names
- When **updating** an existing dependency:
  - Search web to verify compatibility
- For dependencies already in catalog with no changes requested:
  - Assume versions are current

### Testing Strategy

- Write unit tests for all ViewModels (state transitions, event handling)
- Write unit tests for repositories (data operations)
- Use Turbine for testing StateFlow emissions
- Write UI tests for critical flows when specifically requested
- Ensure tests pass before marking feature complete

## Quality Checklist

Before considering a feature complete, verify:

- ✅ `./gradlew ktlintFormat` auto-fixes formatting
- ✅ `./gradlew ktlintCheck` passes
- ✅ `./gradlew assembleDebug` succeeds
- ✅ `./gradlew test` passes
- ✅ Follows style-guide.md patterns (MVVM, naming, state hoisting)
- ✅ No hardcoded strings
- ✅ Files under 300 lines
- ✅ `assembleDebug` builds successfully
- ✅ `test` passes all tests
- ✅ All required sections in design.md are complete
- ✅ Error states are handled in UI
- ✅ Testable composable pattern is followed (Screen + ScreenContent)
- ✅ Files are in correct packages per architecture.md

## Documentation Standards

### Plan Doc Template

```markdown
# <Feature Name> - Plan

## Context
[Why is this needed? What problem does it solve?]

## Scope
**Included:**
- [What's being built]

**Excluded:**
- [What's explicitly not included]

## Approach
[What will be done and WHY this approach over alternatives]

## Tasks
1. [Concrete step with what and why]
2. [Another concrete step]

## Dependencies
[New or updated libraries with verified versions, or delete this section]

## Open questions
[Anything uncertain or needing user input, or delete this section]

## Verification
- `assembleDebug` succeeds after each task
- `test` passes all tests
- Manual: [How to manually verify it works]
```

### Design Doc Template

```markdown
# <Feature Name> - Design

## Overview
[One paragraph: what was built]

## What was built
- `path/to/File.kt`: [Brief description]
- `path/to/Another.kt`: [Brief description]

## Design decisions
- **Decision 1**: [What was chosen and why]
- **Decision 2**: [Trade-offs considered]

## Known gaps
[Anything incomplete or deferred, or delete this section]
```

## Common Workflow Patterns

### Adding a New Screen

1. Plan: Define screen purpose, state shape, user interactions
2. Layer 1: No new dependencies typically
3. Layer 2: Create data models if needed
4. Layer 3: Create ViewModel with state and events
5. Layer 4: Create Screen and ScreenContent composables
6. Layer 5: Test ViewModel state transitions

### Adding a Repository Function

1. Plan: Define function signature, data flow
2. Layer 2: Add DAO method, update repository
3. Layer 3: Update ViewModel to call new repository method
4. Layer 4: Update UI to trigger/display new data
5. Layer 5: Test repository and ViewModel

### Updating Existing Feature

1. Plan: Identify affected files, minimal changes needed
2. Read existing code carefully
3. Make focused changes layer by layer
4. Test after each change
5. Update design doc with changes

## Anti-Patterns to Avoid

- ❌ Writing all files before first build
- ❌ Skipping `assembleDebug` between layers
- ❌ Making unrelated refactors
- ❌ Hardcoding dependency versions
- ❌ Copying code from old projects without adapting
- ❌ Adding features not in the approved plan
- ❌ Ignoring build warnings
- ❌ Committing code that doesn't compile
