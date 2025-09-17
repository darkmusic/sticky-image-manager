# Copilot instructions: Sticky Image Manager

Goal: Make AI agents productive quickly in this JavaFX app by capturing architecture, workflows, and repo-specific conventions.

## Big picture
- Single JPMS module `com.darkmusic.stickyimagemgr` with two runtime roles:
  - Manager app: `ManagerApplication` + `ManagerController`
  - Viewer windows: one `ViewerController` per Stage
- Manager launches N viewers based on JSON; state persists via:
  - Per-config: `ManagerPrefs` + `ViewerPrefs` (JSON file chosen by user)
  - App-level recents: `AppPreferences` at `~/.stickyimagemgr`
- Styling from `src/main/resources/css/main.css`; EXIF orientation via `metadata-extractor`.

## Key code paths
- `ManagerApplication`: boot app, load app prefs, auto-open most recent config.
- `ManagerController`: menu/UI, open/new/save, Launch/Kill, Toggle Decorations, recents, log.
- `ViewerController`: right-click menu (Open/Close), image load, EXIF rotate, sizing via `safeMove`.
- Pref POJOs: `ManagerPrefs`, `ViewerPrefs`, `WinPrefs` (clamps min bounds), `AppPreferences`.
- `module-info.java`: requires JavaFX, ControlsFX, Ikonli, BootstrapFX, Jackson, metadata-extractor; exports package.

## Build & run
- Requires JDK 25 + Maven; Just is provided:
  - build → `just build` (mvnw clean compile)
  - install → `just install`
  - run → `just run` (javafx:run; main class configured)
- On first run, Manager opens the most recent config from `~/.stickyimagemgr` if present.

## Config & persistence
- Manager config JSON fields: `instanceCount`, manager `locationX/Y`, and `viewerPrefList` (array of `ViewerPrefs`).
- `ViewerPrefs`: `locationX/Y`, `sizeW/H`, `imagePath`.
- `AppPreferences` example: `{ "recentFiles": ["/abs/path/Config.json", ...] }`.
- Jackson `ObjectMapper` is used directly; pretty-print on save.

## Runtime behaviors
- Toggle Decorations switches `StageStyle` and applies a 28px `y_offset` hack in undecorated mode; saving is blocked while offset is applied—toggle back before Save.
- Launch fills missing viewer entries with defaults; no `imagePath` → 300x300 window at manager position.
- `safeMove(Point2D, Dimension2D)` updates Stage and ImageView coherently; use it for positioning/sizing.
- Open dialog remembers last-used dir via `ManagerController.lastUsedDirectory`.

## Conventions & changes
- UI is imperative (no FXML). Keep logic in controllers; spawn viewers only via `ManagerController`.
- Persist with existing POJOs and schema; maintain backward-compatible JSON.
- Add styles in `resources/css/main.css` and attach via root stylesheets.

## Extending (examples)
- New Manager action: add `MenuItem` in `createMenuBar()`, implement `handleXxxAction()`, log with `logText(...)`.
- New per-viewer setting: add field to `ViewerPrefs` (and possibly `WinPrefs`), default in `handleLaunchAction()`, apply in `ViewerController.createContent(...)` or `safeMove(...)`.

Acceptance
- Keep `module-info.java` accurate; only add requires/exports if needed.
- Don’t regress decoration toggle/save semantics or EXIF rotation.
- Preserve config JSON compatibility unless explicitly migrating.
