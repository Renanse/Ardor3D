# ardor3d-editor

A visual scene editor for Ardor3D, built with Compose for Desktop. The 3D viewport is a real
`Lwjgl3AwtCanvas` embedded via `SwingPanel`, so everything you see is rendered by the engine
itself.

![Editor screenshot](../editor.png)

## Running

```
./gradlew :ardor3d-editor:run
```

Requires Java 17+. The module builds with its own Kotlin/Compose plugins and is skipped by the
root project's Java conventions.

Note: under WSLg (Linux GUI on Windows), the UI, grid and lines render but triangle meshes do
not — the same is true of the stock `LwjglAwtExample`, so it is an engine/driver interaction
with WSLg's OpenGL stack, not an editor issue. Run on a native desktop for real use.

## Features

- **Hierarchy panel** — live scene tree with selection, right-click context menu
  (Rename / Duplicate / Delete).
- **Multi-select** — ctrl-click toggles membership (hierarchy and viewport), shift-click selects
  a range in the hierarchy. Delete/Duplicate act on the whole selection as one undo step;
  selecting a node covers its descendants so they aren't processed twice.
- **Inspector panel** — name, transform (position / XYZ Euler rotation / scale), mesh info,
  ColorSurface material (diffuse/ambient/specular/emissive, shininess), wireframe toggle, and
  light properties (enabled / intensity / color).
- **Viewport** — WASD + drag fly camera, mouse-wheel dolly, primitive-accurate click selection,
  interact-widget gizmos for translate/rotate/scale, per-light overlay gizmos, reference grid.
- **Undo/redo everywhere** — every mutation goes through a command stack
  (`com.ardor3d.editor.command`). Continuous gestures (slider drags, typing, gizmo drags)
  coalesce into single undo steps. `Ctrl+Z` / `Ctrl+Shift+Z` / `Ctrl+Y`.
- **Persistence** — File > New / Open / Save / Save As using Ardor3D's binary format (`.a3d`),
  plus Wavefront OBJ import. Unsaved changes are flagged in the title bar and confirmed before
  being discarded.
- **Object creation** — GameObject menu: 13 primitive shapes, empty nodes, and
  point/directional/spot lights.

### Keyboard shortcuts

| Key | Action |
| --- | --- |
| `1` / `2` / `3` | Translate / Rotate / Scale mode (viewport focused) |
| `F` | Frame selection (viewport focused) |
| `Delete` | Delete selection (viewport focused) |
| `W A S D` + right-drag | Fly camera (viewport focused) |
| `Ctrl+Z` / `Ctrl+Shift+Z` / `Ctrl+Y` | Undo / Redo (window-wide) |

## Architecture notes

- **Document vs. overlay** — `EditorScene` renders two roots: the *document*
  (`EditorState.sceneRoot`, what the user edits and saves) and an *editor overlay* (grid, light
  gizmos) that never appears in the hierarchy, picking, or saved files.
- **Threading** — the Swing timer drives `FrameHandler.updateFrame()` on the AWT EDT, and
  Compose Desktop dispatches UI events on the EDT too, so scene mutations never race the render
  loop. Document lifecycle actions (open/save/new/import) are queued and executed at the top of
  `update()` so they run with the GL context current.
- **Change notification** — panels observe coarse version counters on `EditorState`
  (`transformVersion`, `structureVersion`, `propertyVersion`, `historyVersion`) that bump only
  when something actually changed; there is no per-frame recomposition.
- **Commands** — `EditorCommand` / `CommandStack` in `com.ardor3d.editor.command`. UI code
  builds `SetterCommand`s with merge keys for coalescing; gizmo drags are captured by
  `GizmoUndoFilter`, an interact-system `UpdateFilter` that records one command per drag.
- **Serialization** — scenes save with `BinaryExporter`. Render materials are not serialized;
  they are re-derived on load with `MaterialUtil.autoMaterials`. `ColorSurface` properties,
  render states, lights and transforms round-trip (covered by `SceneRoundTripTest`).

## Known limitations / next steps

- The inspector edits the primary (first) selected object only.
- No reparenting via drag-and-drop in the hierarchy.
- Scale gizmo is uniform-only (`SimpleScaleWidget`).
- Rotation is edited as XYZ Euler angles and will jump near the attitude (Z) ±90° singularity.
- No camera objects or play mode.
- OBJ is the only model import wired up; COLLADA support exists in `ardor3d-collada` and could
  be added the same way.
