---
type: home
tags:
  - home
---

# SyncPlayer Docs

Local Android music player that syncs metadata (play counts, playlists, favorites) across devices.

---

## Guides

| Doc | Purpose |
|-----|---------|
| [[process]] | Step-by-step feature implementation workflow |
| [[architecture]] | MVVM layers, package structure, data flow |
| [[style-guide]] | Naming conventions, patterns, file organisation |
| [[dependencies]] | Full tech stack with versions and AGP 9 notes |
| [[token-optimization]] | Working efficiently with Claude Code |

---

## Feature Tracker

> Filter by `status` in the Properties panel, or use the Tag Pane to browse `#status/planned`, `#status/complete`, etc.

### Phase 1 — Library ✅
- [[library-browsing-plan]] / [[library-browsing-design]]

### Phase 2 — Playback ✅
- [[playback-plan]] / [[playback-design]]

### Phase 3 — Library → Playback Navigation ✅
- [[library-playback-nav-plan]] / [[library-playback-nav-design]]
- [[bugfixes-phase2-plan]] / [[bugfixes-phase2-design]]

### Phase 4 — Playlists ✅
- [[playlists-plan]] / [[playlists-design]]
- [[bugfix-plan]] *(post-phase bugfixes)*

### Phase 5 — Metadata Tracking ✅
- [[metadata-tracking-plan]] / [[metadata-tracking-design]]

### Phase 6 — Sync ✅
- [[sync-plan]] / [[sync-design]]
- [[fix-firestore-sync-plan]] *(sub-plan: Firestore write fix)*

### Phase 7 — Polish ✅
- [[phase7-plan]] / [[phase7-design]]
- [[open-bugs-plan]] / [[open-bugs-design]]
- [[ui-library-analysis]] / [[data-analysis]] *(audits)*
- [[testing-plan]] / [[testing-design]]
- [[tabs-plan]] / [[tabs-design]]

### Phase 7.5 — UI Redesign ✅
- [[ui-redesign-plan]] / [[visual-design-spec]]

### Phase 8 — External Integrations
- [[widget-plan]] / [[widget-design]] ✅ *Now Playing home screen widget*
- [[android-auto-plan]] / [[android-auto-design]] ✅ *Android Auto browse tree*
- [[m3u-import-export-plan]] 🔲 *planned*

---

## Backlog

| Plan                        | Priority | Status     |
| --------------------------- | -------- | ---------- |
| [[songs-tab-refactor-plan]] | High     | 🔲 planned |
| [[queue-sheet-plan]]        | Medium   | 🔲 planned |
| [[ci-cd-plan]]              | Medium   | 🔲 planned |
| [[improvements-plan]]       | Low      | 📋 backlog |

---

## Project Plan

See [[docs/PLAN]] for the full phase-by-phase project overview with completion checklists.
