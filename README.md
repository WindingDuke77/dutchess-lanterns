# Lantern

A Minecraft 1.12.2 Forge mod by **dutchess77**. Automates area lighting: sweeps up torches
and buries **invisible lights** in the ground as you walk. Built for Tekxit 3.14 Pi but works
in any 1.12.2 pack — EnderIO and Baubles are optional integrations, not requirements.

## Items

| Item | Fuel | Notes |
|---|---|---|
| **Lantern** | 1 Glowstone block per light | The original. Obsidian/glowstone/diamond recipe. |
| **Energy Lantern** | 2,000 FE per light (200k buffer) | Charge in any FE item charger. Nether star recipe. FE-paid lights drop no glowstone. |
| **Torch Lantern** | 1 Torch per light | Places visible torches on the grid instead — a tidy Lantern of Paranoia. Never sweeps torches. |
| **Creative Lantern** | Free | Places plain visible glowstone. Creative menu only. |

## Controls

- **Sneak + Right-Click (air)** — toggle on/off (item glints while active)
- **Right-Click** — load fuel from inventory, topping up from carried backpacks
- **Sneak + Right-Click on a block** — *reclaim*: reverts hidden lights around it, refunding glowstone
- Works held, on the hotbar, or worn in the **Baubles charm slot**
- `/lantern help` in-game for the same info

## Behavior

While active, every half-second it processes a 32x32 area around you:

- Removes torches (configurable whitelist) and returns them to your inventory
- Replaces ground blocks with **`lantern:hidden_light`** — a full solid block at light 15
  whose model renders the block it replaced, so the lighting is invisible
- Placements snap to a **global, world-aligned 6-block grid**; a gap-fill pass then fixes
  any standable spot still dark (walls, overhangs, unplaceable ground), anchored to the
  exact dark spot; the level *you're on* is served, not floors above you
- Only acts where block light ≤ 7; never replaces light sources, ores, containers,
  unbreakables, leaves, or transparent blocks; also lights the ground under water
- Legacy lights from older versions (EnderIO painted glowstone) auto-convert to the
  new block as chunks load, free

Everything is configurable in `config/lantern.cfg` (grid spacing, radius, costs,
dimension blacklist, underwater, migration, and more).

## Debug commands (op)

`/lantern status` · `/lantern why [x y z]` · `/lantern scan [radius]` · `/lantern undo [radius]`

## Building

No installed toolchain needed beyond a portable JDK 8 in `tools/jdk8` (gitignored).
Gradle 4.10.3 cannot run on modern JVMs, so always build with:

```
build.bat
```

Jar lands in `build/libs/`. `deploy.bat` copies the newest jar into the Tekxit instance
and refuses to run while the game is open (swapping the jar under a running game
corrupts its classloader).

If `tools/jdk8` is missing, download a Temurin 8 JDK zip from
<https://api.adoptium.net/v3/binary/latest/8/ga/windows/x64/jdk/hotspot/normal/eclipse>
and extract so `tools/jdk8/bin/javac.exe` exists.

Built against Forge `1.12.2-14.23.5.2847` (newest 1.12.2 build with FG2.3 userdev
artifacts); runs on `14.23.5.2860` as shipped in Tekxit.
