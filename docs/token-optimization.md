# Token Optimization Guide

How to work efficiently with Claude Code and avoid running out of tokens.

## The Problem

Claude Code has a limited context window. Every file read, every response, every piece of documentation consumes tokens. When the context fills up:
- Responses become slower
- Claude may miss important details
- You hit token limits and need to start over

## Quick Wins

### 1. Use .claudeignore

The `.claudeignore` file in the project root excludes files from Claude's context.

**Already excluded for you:**
- Build outputs (`build/`, `*.apk`, `*.aab`)
- Generated code (`**/generated/`, KSP output)
- IDE files (`.idea/`, `*.iml`)
- Gradle cache (`.gradle/`)

**Can be excluded if not needed:**
- Documentation (`docs/`) - uncomment when not working on docs
- Tests (`app/src/test/`) - uncomment when not writing tests
- Resources (`res/drawable/`) - uncomment when not working on UI

**To edit:** Open `.claudeignore` and uncomment lines as needed.

### 2. Be Specific in Prompts

```
❌ "Fix the library feature"
   → Claude reads entire ui/library/ package

✅ "Fix the null check in LibraryViewModel.kt line 42"
   → Claude reads one file, one location
```

```
❌ "Add tests for everything"
   → Claude reads all code to understand what to test

✅ "Add ViewModel tests for LibraryViewModel state transitions"
   → Claude reads LibraryViewModel.kt and writes focused tests
```

### 3. Work in Layers (Already Your Process!)

Your current approach is optimal:
```
Session 1: Plan (write plan.md)
Session 2: Dependencies + Models → build
Session 3: Repository + ViewModel → build  
Session 4: UI composables → build
Session 5: Tests → test
```

Each session focuses on a small piece, using minimal context.

### 4. Start Fresh Conversations

Don't let conversations drag on forever:
- ✅ Feature complete? Start new conversation for next feature
- ✅ Build successful? Start fresh for next layer
- ✅ Context feels cluttered? Summarize and restart

## Advanced Strategies

### Reference, Don't Duplicate

In plan.md files:
```markdown
❌ Don't do this:
## Architecture
[Copy entire MVVM explanation from architecture.md]

✅ Do this:
## Architecture
Follows MVVM pattern (see docs/architecture.md).
Repository → ViewModel → StateFlow → UI.
```

### Link to Examples

```markdown
❌ Don't do this:
## State Management
[Paste entire state management section]

✅ Do this:
## State Management
Uses sealed interface pattern (see LibraryViewModel.kt for example).
```

### Batch Related Changes

```
✅ "Add SongEntity, AlbumEntity, PlaylistEntity to data/local/entity/"
   → One operation, minimal context

❌ "Add SongEntity" → "Now add AlbumEntity" → "Now add PlaylistEntity"
   → Three operations, repeated context
```

### Use Compiler Errors as Guide

Let the build tell you what's needed:
```
✅ "./gradlew assembleDebug failed with 'Unresolved reference: hiltViewModel'
    Fix the missing Hilt dependency"

❌ "Something's wrong with Hilt, figure it out"
   → Claude reads everything trying to diagnose
```

## Session Management

### Good Session Boundaries

Start new conversations when:
- ✅ Feature is complete (plan → implement → test → design doc)
- ✅ Switching to different feature
- ✅ Moving from implementation to testing
- ✅ Context exceeds ~100k tokens (Claude will feel slower)

### Carry Context Forward

When starting fresh, provide minimal context:
```
"Continue library feature. SongRepository and LibraryViewModel are done.
Now implement LibraryScreen UI with song list and filter dropdown."
```

Not:
```
"Here's everything we did in the last 5 sessions..." 
[paste entire conversation history]
```

## Prompt Patterns

### ✅ Token-Efficient Prompts

```
"Add play count tracking:
1. Add playCount field to SongEntity
2. Add incrementPlayCount() to SongDao  
3. Call it when song starts playing
Files: SongEntity.kt, SongDao.kt, PlayerViewModel.kt"
```

**Why it works:** Specific files, clear scope, no exploration needed.

### ❌ Token-Heavy Prompts

```
"I want to track how many times users play songs. 
Figure out where to add this and implement it."
```

**Why it's bad:** Claude must read entire codebase to understand architecture, find relevant files, design solution.

## Monitoring Token Usage

### Signs You're Running Low

- Claude's responses get shorter
- Claude asks to "continue in next message"
- Responses take longer to generate
- Claude suggests starting fresh

### What to Do

1. **Summarize current state:**
   ```
   "We've completed: Song model, SongDao, SongRepository.
   All tests passing. Ready for ViewModel layer."
   ```

2. **Start new conversation** with that summary

3. **Update design doc** so context is preserved in documentation

## Documentation Strategy

### Keep Docs Modular (Already Done!)

Your current structure is optimal:
- `CLAUDE.md` - Core essentials (~1,200 tokens)
- `process.md` - Read when implementing
- `architecture.md` - Read when architecting
- `dependencies.md` - Read when adding deps
- `style-guide.md` - Read when coding

Claude only reads what's needed for each task.

### In Feature Docs

**plan.md - Keep it concise:**
```markdown
## Context
Users need song filtering by artist for better navigation.

## Approach  
Add dropdown to LibraryScreen. Use existing filter pattern from PlaylistScreen.

## Tasks
1. Add filter state to LibraryViewModel
2. Create FilterDropdown composable
3. Wire up in LibraryScreen

See style-guide.md for naming, architecture.md for state patterns.
```

**design.md - Link to code:**
```markdown
## What was built
- LibraryViewModel.kt: Added `artistFilter` state
- FilterDropdown.kt: Reusable dropdown component
- LibraryScreen.kt: Integrated filter UI

See the code for implementation details.
```

## .claudeignore Best Practices

### Default Exclusions (Always Ignore)

These are in your `.claudeignore` and should stay:
```
build/
.gradle/
*.apk
*.iml
.idea/
```

### Conditional Exclusions (Uncomment as Needed)

Working on business logic? Uncomment:
```
# app/src/main/res/drawable/
# app/src/main/res/mipmap-*/
```

Done writing tests? Uncomment:
```
# app/src/test/
# app/src/androidTest/
```

Done with documentation? Uncomment:
```
# docs/
```

### Never Ignore

Keep these always visible:
- Source code (`*.kt`)
- Build configs (`build.gradle.kts`, `libs.versions.toml`)
- String resources (`strings.xml` - only ~20KB)
- Current plan/design docs

## Real-World Examples

### ❌ Token-Heavy Session

```
You: "Build the library feature"

Claude: [Reads entire ui/ package, data/ package, model/ package]
        [Writes plan.md]
        [Writes all models]
        [Writes all DAOs]
        [Writes repository]
        [Writes ViewModel]
        [Writes all UI composables]
        [Writes all tests]
        
Token usage: 150k+ (hits limit, needs restart)
```

### ✅ Token-Efficient Sessions

```
Session 1:
You: "Write plan.md for library feature - browsing songs by artist/album"
Claude: [Reads architecture.md, process.md]
        [Writes plan.md]
Token usage: ~15k

Session 2:  
You: "Approved. Implement data layer (Song model, DAO, repository)"
Claude: [Reads dependencies.md, architecture.md]
        [Creates SongEntity.kt, SongDao.kt, SongRepository.kt]
        [Tests build]
Token usage: ~20k

Session 3:
You: "Add ViewModel layer with song list state and filtering"
Claude: [Reads SongRepository.kt, style-guide.md]
        [Creates LibraryViewModel.kt, LibraryUiState.kt]
        [Tests build]
Token usage: ~18k

Session 4:
You: "Build LibraryScreen UI - song list with artist filter"
Claude: [Reads LibraryViewModel.kt, style-guide.md]
        [Creates LibraryScreen.kt, SongListItem.kt]
        [Tests build]
Token usage: ~22k

Session 5:
You: "Add tests for LibraryViewModel state transitions"
Claude: [Reads LibraryViewModel.kt]
        [Creates LibraryViewModelTest.kt]
        [Runs tests]
Token usage: ~15k

Total: ~90k across 5 focused sessions vs 150k+ in one bloated session
```

## Quick Reference

**Before starting work:**
- [ ] Update `.claudeignore` for current task
- [ ] Reference specific files in prompt
- [ ] Check if starting fresh is better than continuing

**During work:**
- [ ] One layer at a time
- [ ] Build after each step
- [ ] Keep responses focused

**After completing work:**
- [ ] Write design doc (captures context)
- [ ] Start fresh for next feature
- [ ] Restore `.claudeignore` excludes if needed

## Emergency: Hit Token Limit

If you hit the limit mid-task:

1. **Summarize current state:**
   - What's complete
   - What's in progress
   - Next step

2. **Commit or save work**

3. **Start new conversation:**
   ```
   "Continuing library feature implementation.
   Completed: Models, DAOs, Repository, ViewModel
   In progress: LibraryScreen UI (see plan.md task 4)
   Next: Add SongListItem composable to display songs"
   ```

4. **Reference existing files:**
   ```
   "Follow the pattern in LibraryViewModel.kt"
   ```
   
   Not:
   ```
   "Here's the LibraryViewModel code: [paste entire file]"
   ```
