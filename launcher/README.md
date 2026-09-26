# Radinee Client Launcher

A standalone Windows launcher shell for **Radinee Client**.

The launcher is intentionally separate from the existing Fabric mod project. It will eventually manage a dedicated Radinee game instance, Java runtime, client files, profiles, RAM and launch arguments.

## Project layout

- `launcher/` — standalone launcher application
- `src/` — current Fabric prototype
- `.github/workflows/` — automated builds

The launcher does not modify the user's normal Minecraft installation.

## Name

Radinee Client

## Planned launcher flow

1. Select Minecraft version.
2. Select or install the bundled Radinee Client instance.
3. Configure RAM.
4. Launch Radinee Client in its own game directory.
5. Keep visual modules/settings independent from other Minecraft installations.
