# SyncPlayer Visual Design Spec

## Design Philosophy

**Content-forward, chrome-minimal.** Album art and artist portraits are the visual heroes. UI chrome steps back. A single accent color signals active state — used sparingly, never decoratively.

**Zune-inspired.** Bold typography, large imagery, confident negative space. The UI doesn't apologize for taking up space.

**Glass language.** Frosted glass overlays at layer boundaries. Static blur bitmaps — not continuous GPU effects. Performance first.

---

## Color System

### Base Palette

| Token | Value | Usage |
|-------|-------|-------|
| Background | `#111113` (near-black) | Screen background |
| Surface | Material `sys/dark/surface` | Cards, sheets |
| Surface Tint | Material `sys/dark/surface-tint` | Elevated surfaces |
| On Surface | Material `sys/dark/on-surface` | Primary text |
| On Surface Variant | Material `sys/dark/on-surface-variant` | Secondary text |
| **Accent** | `#ff1d58` (hot pink) | Active state only |

### Accent Usage Rules

- Maximum **1–2 accented elements per screen**
- Accent signals: currently playing item, active tab, active playback controls
- Never use accent for decoration — only for state
- When a song is playing: title text, subtitle text, border, and playback icon all turn accent
- Everything else stays white/grey

### Frosted Glass Recipe

Used for: info overlays on album art/artist portraits, MiniPlayer background, Queue sheet background.

```
Fill: sys/dark/surface at 60–70% opacity
Effect: Background blur, radius 8–12dp
```

In Compose:
```kotlin
// Static blur snapshot approach (API 31+)
RenderEffect.createBlurEffect(radiusPx, radiusPx, Shader.TileMode.CLAMP)
// Pre-31 fallback: Canvas Gaussian blur on bitmap snapshot
```

---

## Typography

**Font family:** Nunito Sans Variable (5 axes)

| Style | Size | Weight | Usage |
|-------|------|--------|-------|
| Display | 32sp | 800 ExtraBold | App name "SyncPlayer" in top bar |
| Title Large | 22sp | 700 Bold | Active tab label, screen headings |
| Title Medium | 16sp | 600 SemiBold | Song title, playlist name, album title |
| Body Medium | 14sp | 400 Regular | Artist name, album name in subtitle |
| Label Small | 12sp | 400 Regular | Metadata (song count, duration, timestamps) |

Active tab label is **larger** than inactive — not just a color change. Inactive tabs use Title Medium, active tab uses Title Large.

---

## Spacing & Grid

- Base unit: **8dp**
- Screen horizontal padding: **16dp**
- List item height: **72dp** (song items)
- Album art thumbnail (in list): **56×56dp**, corner radius **4dp**
- Component gap: **8dp**
- Section gap: **16dp**

---

## Navigation Structure

No bottom navigation bar. Navigation is tab-based at the top.

### Top App Bar
- App name left, search icon right
- Height: ~56dp
- Background: transparent over content, blurs on scroll

### Tab Row (Scrollable)
- 6 tabs: **History | Faves | Songs | Albums | Artists | Playlists**
- Active tab: accent color + larger text (Title Large)
- Inactive tab: white at reduced opacity (Title Medium)
- Tabs overflow right — visual cue for scrollability
- No underline indicator — size + color do the work

### MiniPlayer
- Persistent floating pill at bottom of screen
- Sits above the screen edge with horizontal margin (~8dp each side)
- Height: ~72dp, corner radius: 36dp (fully rounded pill)
- Contains: album art | track info | skip prev | play/pause | skip next
- Play/pause button has a highlighted background in active states (accent-tinted)
- Tapping expands to Now Playing (not navigation — AnimatedContent expansion)

---

## Component Specs

### Song Item (72dp height)

```
[56×56 album art] [title bold 16sp / artist•album 12sp] [optional: star] [⋮ 48×48]
```

**States:**
- Default: white text
- Playing: accent text on title, subtitle, and any icon; album art gets accent border (1.5dp inside)
- Selected: accent border on entire item (1dp inside)
- Reorderable: replaces ⋮ with drag handle (unfold_more icon)
- Deletable: replaces ⋮ with trash icon
- Rated: adds star rating display between text and ⋮ (non-interactive display)

### Album Item (grid, ~152dp wide)

```
[full-width square album art]
[title 14sp bold]
[artist 12sp secondary]
```

Album art has subtle corner radius (4dp). No border in default state. Pink border in playing states. Play/pause button overlaid on art (subtle, power-user discoverable).

### Artist Item (grid, ~152dp wide)

```
[circular portrait, full width]
[frosted glass pill: play icon | artist name | ⋮]
```

Pill sits below the circle with slight overlap. Fully rounded ends. Frosted glass fill.

### Playlist Item (72dp height)

```
[56×56 2×2 collage] [name bold 16sp / #songs • duration 12sp] [▷] [⋮]
```

Collage is 4 album art thumbnails in a 2×2 grid at 28×28dp each.

### MiniPlayer

```
[56×56 album art] [title / artist•album — fill] [⏮] [▷/⏸] [⏭]
```

Pill shape. Frosted glass background. Play/pause has accent-tinted square background in active state.

---

## Screen Layouts

### Songs / Albums / Artists / Faves (List Screens)

```
[Top App Bar: SyncPlayer + search]
[Scrollable Tab Row]
[Sticky Sub-header: sort label + shuffle + play]  ← sticks on scroll
[List / Grid content]
[MiniPlayer floating above bottom edge]
```

Alphabet fast-scroll index on right edge of Songs, Albums, Artists screens (text-only, no Figma component needed — standard Android pattern).

### Album Detail

```
[Top App Bar: SyncPlayer + search + back chevron]
[Hero: full-width album art ~240dp tall]
  [frosted glass strip top-left: back ↓ | album name | artist]
  [frosted glass strip bottom-right: shuffle + play]
[Song list — no alphabet index]
[MiniPlayer]
```

Hero shrinks as user scrolls down (parallax or simple clip).

### Artist Detail

```
[Top App Bar: SyncPlayer + search]
[Hero: full-width circular-cropped portrait ~240dp tall]
  [frosted glass strip top-left: back ↓ | artist name | #albums • #songs]
  [frosted glass strip top-right: ⋮]
[Segmented tabs: Songs | Albums — bottom edge of hero]
  [shuffle + play — right of tabs]
[Song list or Album grid]
[MiniPlayer]
```

### Playlist Detail

```
[Top App Bar: SyncPlayer + search]
[Header: playlist name | #songs • duration]
[Sub-header: shuffle + play]
[Deletable + reorderable song list]
[MiniPlayer]
```

Future improvement: hero collage image at top (2×2 expanded to full width).

### History (Dashboard)

```
[Top App Bar: SyncPlayer + search]
[Scrollable Tab Row — History active]
[Expandable section: Recently Played Songs]
  [section header: label + shuffle + play + chevron]
  [2–3 song items preview]
[Expandable section: Recently Played Albums]
  [2-column album grid preview]
[Expandable section: Recently Played Artists]
  [circular portrait grid preview]
[MiniPlayer]
```

### Now Playing

```
[Back chevron top-left] [Queue icon top-right]
[Full-screen album art hero]
[Seek bar with timestamps]
[Star rating (5 stars) + heart favorite]
[Track title bold / album / artist]
[Playback controls: repeat | ⏮ | ▷/⏸ | ⏭ | shuffle]
```

Controls bar has a frosted glass pill background. Play/pause button is larger than skip buttons.

### Queue Sheet (Bottom Sheet over Now Playing)

```
[Queue header + clear all 🗑 + collapse ↓]
[Reorderable song list]
  [first item: accent/playing state]
  [remaining: default state]
```

---

## Transitions & Animation

### MiniPlayer → Now Playing
- **Not** a navigation transition
- `AnimatedContent` or custom expansion animation
- MiniPlayer expands upward to fill screen
- Shared element: album art grows from 56dp thumbnail to full-width hero

### Screen transitions
- Standard Material shared element transitions via `SharedTransitionLayout` (Compose 1.7+)
- Album art shared element from Albums grid → Album Detail
- Artist portrait shared element from Artists grid → Artist Detail

### Color extraction
- `androidx.palette:palette-ktx` for album art dominant color
- `DarkVibrantSwatch` preferred, `DarkMutedSwatch` fallback
- Used for: subtle background tint on Now Playing screen
- `animateColorAsState` for smooth transitions between tracks

### Scroll behavior
- Hero images on detail screens shrink/parallax on scroll
- Top App Bar gains background blur on scroll
- Sub-header sticks below tab row on scroll

---

## Implementation Notes

### Frosted Glass (Compose)
```kotlin
// API 31+ 
Modifier.graphicsLayer {
    renderEffect = BlurEffect(radiusPx, radiusPx, TileMode.Clamp)
}
// Combined with semi-transparent fill:
Box(modifier = Modifier.background(Color(0xFF1C1C1E).copy(alpha = 0.65f)))
```

### Accent Color Token
```kotlin
// In Theme.kt — extend MaterialTheme
val AccentColor = Color(0xFFff1d58)
```

### Font
```kotlin
// Nunito Sans Variable via Google Fonts
// In res/font/ or via downloadable font
val NunitoSans = FontFamily(
    Font(R.font.nunito_sans, weight = FontWeight.Normal),
    Font(R.font.nunito_sans_bold, weight = FontWeight.Bold),
    Font(R.font.nunito_sans_extrabold, weight = FontWeight.ExtraBold),
)
```

### Alphabet Fast Scroll
Standard `LazyColumn` with a custom overlay composable on the right. Not a separate component — implement directly on list screens that need it (Songs, Albums, Artists).

---

## What Doesn't Change

- MVVM architecture — no changes
- Room database — no changes  
- Sync layer — no changes
- Navigation routes — same routes, updated composables
- ViewModel logic — same events and state shapes
- Repository layer — untouched

The redesign is **purely UI layer** — composables and theme only.
