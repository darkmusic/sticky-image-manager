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
- `ManagerController`: menu/UI, open/new/save, Launch/Kill, recents, log; always launches viewers as undecorated.
- `ViewerController`: right-click menu (Open/Close), drag-and-drop to load image, EXIF rotation baked into pixels, contain scaling, custom undecorated move/resize.
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
- Always-undecorated viewers with custom move/resize: drag inside to move; drag edges/corners to resize; min size enforced.
- Launch fills missing viewer entries with defaults; no `imagePath` → 300x300 window at manager position.
- Positions and sizes are saved/restored as-is (no decoration-dependent offsets).
- EXIF rotation is applied by generating a rotated image (no rotate transforms), so aspect/sizing are correct at first render.
- Image sizing: contain mode via `ImageView` with `preserveRatio=true` and both `fitWidth` and `fitHeight` bound to the container; container is clipped to avoid spillover.
- After loading a new image, a shrink-only auto-fit adjusts the window slightly to remove initial letterboxing (never grows the window).
- Drag-and-drop: drop image files (png/jpg/jpeg/gif/bmp) from the OS onto a viewer to load; last-used directory is updated.
- Open dialog remembers last-used dir via `ManagerController.lastUsedDirectory`. "Open" reuses the existing scene/handlers (no reset), so move/resize remains active.

## Conventions & changes
- UI is imperative (no FXML). Keep logic in controllers; spawn viewers only via `ManagerController`.
- Persist with existing POJOs and schema; maintain backward-compatible JSON.
- Add styles in `resources/css/main.css` and attach via root stylesheets.
 - Viewers are always undecorated; do not reintroduce decoration toggles or y-offset hacks.
 - For image type support, update `ViewerController.isAcceptableImagePath()` if you add extensions.

## Extending (examples)
- New Manager action: add `MenuItem` in `createMenuBar()`, implement `handleXxxAction()`, log with `logText(...)`.
- New per-viewer setting: add field to `ViewerPrefs` (and possibly `WinPrefs`), default in `handleLaunchAction()`, apply in `ViewerController.createContent(...)`.
- Add image interactions: hook into `ViewerController.installDragAndDropHandlers()` or context menu to support more sources.

Acceptance
- Keep `module-info.java` accurate; only add requires/exports if needed.
- Don’t regress contain scaling, EXIF rotation correctness, or undecorated move/resize.
- Preserve config JSON compatibility unless explicitly migrating.
