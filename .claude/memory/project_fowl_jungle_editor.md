---
name: project-fowl-jungle-editor
description: Architecture and data context for the fowl-jungle-editor Compose Multiplatform desktop tool
metadata: 
  node_type: memory
  type: project
  originSessionId: ffaf33a1-d2f7-40ea-96ee-7c6f749dacdc
---

Editor for the Fowl Jungle libGDX RPG game. Repo at `/home/nate/Desktop/git/fowl-jungle-editor`. Source game data at `/home/nate/Desktop/git/fowl-jungle/assets/json/`.

**Why:** The game uses 50+ JSON files as its entire content layer. The editor allows editing all of them without hand-editing JSON.

**Targets:** Linux, Windows, macOS via Compose Multiplatform desktop (KMP).

## Runtime / Build

- Gradle run task working dir set to repo root via `afterEvaluate { tasks.named<JavaExec>("run") { workingDir = rootProject.projectDir } }` in `composeApp/build.gradle.kts`
- `sample/` dir at repo root contains a minimal loadable project (`json/` subdir is what the app opens). Sample files are also embedded as classpath resources in `src/desktopMain/resources/sample/` so Initialize can restore them.

## App Architecture

- `AppState` — top-level mutable state; owns `ProjectState`, `MapEditorState`, all dialog flags
- `ProjectState` — holds loaded file lists per editor type; `open(dir)` loads everything; `close()` clears all
- `rememberAppState()` — `LaunchedEffect(Unit)` opens saved project or sample on startup via `defaultProjectDir()`
- `AppPrefs` — single Java Preferences node `"fowl-jungle-editor"`; stores `lastProjectDir`
- `initialize()` on AppState: clears AppPrefs, resets all state, calls `restoreSample()` then reopens sample
- Escape key → opens "Main Menu" dialog (gear icon in menu bar also triggers it)
- Nav rail buttons disabled when no files exist for that editor type (`dest.enabled && accordionLabels.isNotEmpty()`)

## Project File Structure (what the app opens)

```
<project-root>/           ← this dir is opened (e.g. assets/json/ or sample/json/)
  world/maps/*.json       → Map Editor
  equipment/*.json        → Equipment Editor
  weapons/*.json          → Weapons Editor
  items/*.json            → Items Editor
  weapons/*_recipes.json  → Recipes Editor
  items/*_recipes.json    → Recipes Editor
  lifeform/skills.json    → skill name lookup
<project-root>/../tiles/*.png      → minimap tile image options
<project-root>/../textures/*.png/jpg → FPV texture options
```

## Editor Status

| Editor | Status |
|--------|--------|
| Map Editor | Partial — canvas, tile props, auto-placement done; painting/wand tools not yet built |
| Equipment Editor | Partial — read-only list view; CRUD (add/edit/delete) not yet implemented |
| Weapons Editor | Partial — read-only list view; CRUD not yet implemented |
| Items Editor | Partial — read-only list view; CRUD not yet implemented |
| Recipes Editor | Partial — read-only list view; CRUD not yet implemented |
| Event Editor | Stub (disabled button, empty files in `editors/map/event/`) |
| Troop Editor | Stub (disabled button, empty files in `editors/map/troop/`) |
| World/Battle/Troops/Lifeforms | Disabled nav buttons, not started |

## Map Editor Key Details

- Canvas: DrawScope extensions — `drawGrid`, `drawTiles`, `drawMarkers`, `drawSelection`
- `CanvasGrid` computed from tile bounds + `MapBounds` + 1-tile padding
- Chevron expansion: overlay Box 80dp larger than canvas, no per-button offset needed
- Wall rendering: solid = boundary+no ingress, dashed = boundary+ingress, dotted = no boundary+no ingress
- Troop/event dots only drawn when `id.isNotBlank()`
- Tile panel: Ingress + Boundaries side-by-side compass layout (`DirectionToggles`), negative spacedBy(-12dp) to pack checkboxes
- Auto-placement: `autoAddBoundaries` (BFS for FPV texture), `autoAddTiles` (BFS for type/minimap/floor/ceiling), `autoUpdateAdjacentTiles`
- FPV slots: NORTH_BOUNDARY, EAST_BOUNDARY, SOUTH_BOUNDARY, WEST_BOUNDARY, CEILING, FLOOR, CLOSE_FOREGROUND, FOREGROUND, FAR_FOREGROUND
- MapLegend: floating bottom-right of canvas, hideable via checkbox in no-tile-selected panel

## JSON Data Conventions

- IDs: kebab-case e.g. `"skill-weapon-sword"`, `"1h-dagger-bronze"`
- Map coordinates: `"x,y"` string as object key
- `ingress`: which directions a tile can be **entered from** adjacent tiles (true = can enter from that side)
- `drawBoundaries`: wall rendering flags (true = wall drawn on that side)
- damageTypes values must sum to 1.0
- Modifier operations: ADD or MULTIPLY
- Level-keyed maps: integer level as string key e.g. `"4": [...]`

**How to apply:** Use as source-of-truth for data model classes and editor UI design.
