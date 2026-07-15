# Dutchess Lanterns

A Minecraft mod by **dutchess77** (mod id `lantern`). Automates area lighting: sweeps up
torches and buries **invisible lights** in the ground as you walk. Also ships manual Glow
Wands, an upgrade bench, and Darkness Wards to keep areas dark on purpose.

## Which version do I need?

Pick the jar that matches your Minecraft version and mod loader. Every release on the
[releases page](https://github.com/WindingDuke77/dutchess-lanterns/releases) contains one
jar per supported version, named `lantern-<loader>-<mc version>-<mod version>.jar`.

| Minecraft | Loader | Jar to download | Project folder | Notes |
|---|---|---|---|---|
| 1.21.1 | NeoForge | `lantern-neoforge-1.21.1-<version>.jar` | [`neoforge-1.21.1/`](neoforge-1.21.1/) | Curios integration (optional) |
| 1.12.2 | Forge | `lantern-forge-1.12.2-<version>.jar` | [`forge-1.12.2/`](forge-1.12.2/) | The original; Baubles + EnderIO integration (optional). Built for Tekxit 3.14 Pi |

The mod must be installed on **both client and server** (it registers blocks and items).

All versions share the same mod version number — `4.0.0` of the 1.12.2 jar and `4.0.0`
of the 1.21.1 jar have the same features wherever the platforms allow.

## What it does (all versions)

- While active, every half-second it processes the area around you: removes torches
  (configurable whitelist, returned to your inventory) and replaces ground blocks with
  `lantern:hidden_light` — a full solid block at light 15 whose model renders the block
  it replaced, so the lighting is invisible.
- Placements snap to a global, world-aligned 6-block grid, with a gap-fill pass for
  spots the grid can't serve (walls, overhangs, unplaceable ground).
- Fuel: Glowstone (Lantern), Forge Energy (Energy Lantern), Torches (Torch Lantern,
  places visible torches instead), or free (Creative/Dev items).
- Lantern Bench socketed upgrades (Range / Efficiency / Capacity, tiers I–IV),
  Darkness Wards to exclude areas, Glow Wands for manual placement.
- `/lantern help` in game, `/lantern status|why|scan|undo` for op debugging.
- Everything configurable: grid spacing, radius, costs, dimension blacklist, and more.

See each project folder's README for version-specific details (config file location,
optional-mod integrations, build instructions).

## Repository layout

One self-contained project folder per Minecraft version + loader. They do not share
build tooling (a 1.12.2 Forge toolchain and a modern NeoForge toolchain cannot coexist
in one Gradle build), but they share design: same package layout, class names, registry
names, config keys, assets, and behavior. That correspondence is what makes new ports
mechanical — see [PORTING.md](PORTING.md) for the step-by-step guide to bringing the mod
to another Minecraft version or loader.

```
forge-1.12.2/      Forge 1.12.2 project  (Gradle 4 / ForgeGradle 2.3 / JDK 8  — build with build.bat)
neoforge-1.21.1/   NeoForge 1.21.1 project (Gradle 8 / ModDevGradle / JDK 21 — build with build.bat)
PORTING.md         How to port to a new Minecraft version / loader
```

## Building

Each project folder has its own `build.bat` that pins the right JDK and Gradle — always
build with it, never with a globally installed Gradle. Jars land in
`<project>/build/libs/`. Releases are built and published automatically by GitHub
Actions when the version bumps on master.
