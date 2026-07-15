# Dutchess Lanterns — NeoForge 1.21.1

The NeoForge 1.21.1 port of Dutchess Lanterns (mod id `lantern`). See the
[root README](../README.md) for what the mod does and
[PORTING.md](../PORTING.md) for how this port relates to the other versions.

## Differences from the 1.12.2 original

- **Curios** replaces Baubles: lanterns can be worn in any `charm` slot
  (datapack tag + reflection; Curios is optional, no hard dependency).
- **Upgrade items are per-tier** (`lantern:range_upgrade_t1` … `_t4` etc.) because
  1.13+ removed item metadata. Recipes and the bench work the same.
- **No legacy EnderIO light migration** — that only ever applied to old 1.12.2 worlds.
- Item state (active flag, fuel charge, FE, socketed upgrades) lives in **data
  components** instead of NBT; behavior is unchanged.
- Config is TOML: `config/lantern-common.toml` — same keys as the 1.12.2
  `lantern.cfg`, except `dimensionBlacklist` takes dimension ids
  (`"minecraft:the_nether"`) instead of numeric ids.

## Building

No installed toolchain needed. `build.bat` pins JDK 21 (a portable one in
`tools/jdk21` if present, otherwise Prism Launcher's `java-runtime-delta`) and uses the
portable Gradle in `tools/gradle-8.10.2`.

```
build.bat
```

Jar lands in `build/libs/lantern-neoforge-1.21.1-<version>.jar`.

If `tools/gradle-8.10.2` is missing, download
<https://services.gradle.org/distributions/gradle-8.10.2-bin.zip> and extract it so
`tools/gradle-8.10.2/bin/gradle.bat` exists. If you have no JDK 21, extract a Temurin 21
JDK to `tools/jdk21` (<https://api.adoptium.net/v3/binary/latest/21/ga/windows/x64/jdk/hotspot/normal/eclipse>).

`gradle.properties` pins `org.gradle.java.home` to the local JDK 21 — adjust or delete
that line if your JDK lives elsewhere (CI strips it).

## Dev runs

`tools\gradle-8.10.2\bin\gradle.bat runClient` / `runServer` (with `JAVA_HOME` set to a
JDK 21) launch a development instance.
