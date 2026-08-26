# Echo of the Void

Echo of the Void is a NeoForge horror-atmosphere mod for Minecraft 1.21.1.
It turns familiar Minecraft rules into a gradual, unpredictable horror campaign built around
observation, sound, shared anomalies and rare high-pressure encounters.

The standard campaign is paced over approximately 50 Minecraft days. An extra-long 100-day
mode uses the same normalized narrative arc and can be enabled with
`uncanny.campaign.extraLong100Days=true` in the generated common configuration.

## Downloads
Use official builds:
- Repository: https://github.com/RaysorTheorycrafting/Echo-of-the-Void
- Releases: https://github.com/RaysorTheorycrafting/Echo-of-the-Void/releases
- CurseForge: https://www.curseforge.com/minecraft/mc-mods/echo-of-the-void/files

For modpack integration, always use release assets (`.jar`) instead of source snapshots.

## Installation

- Install Minecraft 1.21.1 and NeoForge 21.1.219 or newer in the 21.1 line.
- Use Java 21.
- Put `EchoOfTheVoid-2.0.0.jar` in the `mods` folder.
- Install the same JAR on the server and every connecting client. No external mod dependency is required.

Existing 1.1.1 worlds remain supported. Back up important worlds before updating any modded instance.

## Build From Source
Requirements:
- Java 21
- Gradle Wrapper

Ordinary development build:

```powershell
.\gradlew.bat clean build
```

Release artifact and SHA-256 sidecar:

```powershell
.\gradlew.bat clean build releaseChecksum -PreleaseBuild=true
```

## Project Layout
- `src/` -> mod source code and resources
- `Changelog/` -> release notes and changelogs

## Compatibility
- Minecraft: 1.21.1
- Loader: NeoForge 21.1.219+
- Java: 21
- Environment: integrated client and dedicated server

## License
All Rights Reserved.
