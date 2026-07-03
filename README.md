# Lantern

A Minecraft 1.12.2 Forge mod by **dutchess77** for Tekxit 3.14 Pi (or any pack with EnderIO).

The Lantern sweeps up torches and buries invisible lighting in the ground as you walk:

- **Sneak + Right-Click** — toggle the Lantern on/off (it glints while active).
- **Right-Click** — load Glowstone blocks from your inventory into its internal buffer
  (shown as the yellow durability bar, holds 512 by default).
- While active (held or anywhere on your hotbar) it continuously processes the area
  around you:
  - Removes torches and returns them to your inventory.
  - Replaces the top ground block with EnderIO **painted glowstone**, painted to look
    exactly like the block it replaced — invisible, full-brightness lighting.
  - Only lights spots that are actually dark (block light ≤ 7 by default).
  - Placements snap to a **global, world-aligned grid** (every 6 blocks by default) so
    coverage stays regular no matter where you walk. If a grid point is obstructed it
    shifts by one block to the nearest usable column.
  - Each light costs **1 Glowstone block** — taken from the buffer first, then your
    inventory. It never replaces ores, containers, or unbreakable blocks.

Everything (grid spacing, buffer size, light threshold, radius, torch list, block
blacklist) is configurable in `config/lantern.cfg`.

## Recipe

```
 . iron .
iron torch iron
 . glowstone .
```

## Building

Requires no installed toolchain except the bundled portable JDK 8 (`tools/jdk8`,
downloaded separately — see below). Gradle 4.10.3 cannot run on modern JVMs, so use:

```
build.bat
```

which sets `JAVA_HOME` to `tools/jdk8` and runs `gradlew build`. The jar lands in
`build/libs/lantern-<version>.jar`.

If `tools/jdk8` is missing, download a Temurin 8 JDK zip from
<https://api.adoptium.net/v3/binary/latest/8/ga/windows/x64/jdk/hotspot/normal/eclipse>
and extract it so that `tools/jdk8/bin/javac.exe` exists.

Built against Forge `1.12.2-14.23.5.2847` (the newest 1.12.2 build with FG2.3 userdev
artifacts); runs on `14.23.5.2860` as shipped in Tekxit.
