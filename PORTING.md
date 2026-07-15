# Porting Dutchess Lanterns to a new Minecraft version / loader

Each supported version lives in its own self-contained project folder
(`<loader>-<mcversion>/`). Ports are deliberately parallel: same packages, same class
names, same registry names, same config keys, same lang keys, same assets. Port by
copying the closest existing project and adapting, not by redesigning.

## Ground rules (user-confirmed design decisions — keep them)

1. **No compile-time dependencies on other mods.** Integrations are reflection-based
   (see `compat/CuriosCompat.java` in 1.21.1, `compat/BaublesCompat.java` in 1.12.2)
   or datapack-based (the Curios `charm` tag). The jar must load with no other mod
   present.
2. **Behavior parity.** Grid math, gap-fill, never-replace rules (light sources, ores,
   containers, unbreakables, leaves, transparents), fuel semantics (FE-paid lights drop
   no glowstone), ward suppression, and `/lantern` debugging all behave identically
   across versions. The `logic/` package is the reference: port it faithfully.
3. **Registry names never change** (`lantern:hidden_light`, `lantern:lantern`, ...).
   Exception: the upgrade items became per-tier ids (`range_upgrade_t1`..`_t4`) in
   1.21+ because item metadata no longer exists.
4. **Config keys never change** — a user's config transfers between versions. New
   platform config systems must map onto the same key names (see how
   `LanternConfig.java` in 1.21.1 bakes a `ModConfigSpec` into the same static fields
   the 1.12.2 code read).
5. **One shared mod version** across all projects; bump every project together.
   The version lives in `build.gradle`/`gradle.properties` AND `Lantern.VERSION`.

## Step-by-step for a new port

1. **Copy the closest project folder** (for any modern version, start from
   `neoforge-1.21.1/`). Rename to `<loader>-<mcversion>/`.
2. **Toolchain first.** Update `gradle.properties` (`neo_version` or equivalent),
   `settings.gradle`, and `build.bat` (which JDK the version needs). Get the empty-ish
   scaffold (`Lantern.java` + registries + config) compiling and the mod booting a
   dedicated server before porting gameplay code.
3. **Port in this order** (matches the dependency flow):
   1. `LanternConfig` (same keys), `LanternDataComponents` (or NBT equivalent),
      `ModBlocks` / `ModItems` / `ModMenus`
   2. `logic/` (pure gameplay: SurfaceScanner, LightPlacer, TorchSweeper,
      WardRegistry, PlaceResult) + `handler/LanternTickHandler` + `fx/SparkleManager`
   3. `item/` (LanternItem hierarchy, LanternUpgrades, UpgradeItem)
   4. `block/` (three blocks + block entities + hidden-light camo model in `client/`)
   5. `gui/` + `network/` + `command/`
   6. `compat/` last — always optional, always reflection/datapack
4. **Resources:** assets are nearly version-independent; recipes/advancements live
   under `data/` with the modern schema, lang is JSON. Copy from the newest project
   and adjust schema changes only.
5. **Wire CI.** Add a build job for the new folder in
   `.github/workflows/release.yml` and add the jar to the release step. Jar naming
   must follow `lantern-<loader>-<mcversion>-<modversion>.jar` (set via
   `archivesBaseName` / `base.archivesName`).
6. **Update the version table** in the root `README.md`.

## Version-specific gotchas already solved (steal from these)

- **Hidden-light camouflage** (the heart of the mod): 1.12.2 uses extended block
  states + a wrapping `IBakedModel`; 1.21.1 uses `ModelData`/`ModelProperty` + an
  `IDynamicBakedModel` swapped in during `ModelEvent.ModifyBakingResult`, plus
  NeoForge's `getAppearance` for other mods' facade queries. Whatever the target
  platform, the pattern is: block entity stores the mimicked `BlockState`, a wrapping
  baked model renders the mimic's quads, block colors delegate to the mimic.
- **Worn-slot integration:** Baubles (1.12) implemented an interface; Curios (1.21+)
  needs only a datapack tag (`data/curios/tags/item/charm.json`) plus reflection to
  *read* worn stacks in the tick handler.
- **Item state:** 1.12 NBT ↔ 1.21 data components. Semantics to preserve: a stack
  with no charge tag/component counts as FULL (creative/JEI/give stacks), crafted
  stacks carry explicit charge 0.
- **1.12 item metadata** (upgrade tiers) → separate items per tier.
- **Energy:** Forge Energy ↔ NeoForge energy capability backed by the ENERGY
  component (`ComponentEnergyStorage`).

## Build toolchains

| Project | Gradle | Plugin | JDK | Notes |
|---|---|---|---|---|
| forge-1.12.2 | 4.10.3 (wrapper) | ForgeGradle 2.3 | 8 (portable, `tools/jdk8`) | Gradle 4 dies on modern JVMs — only build via `build.bat`. Forge 2847 userdev, runs on 2860. |
| neoforge-1.21.1 | 8.10.2 (wrapper + `tools/gradle-8.10.2`) | ModDevGradle 2 | 21 (`tools/jdk21` or Prism's `java-runtime-delta`) | `gradle.properties` pins `org.gradle.java.home`; CI strips the pin. |

`tools/` folders are gitignored — each machine downloads its own portable toolchain
(see each project's README).
