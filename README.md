# android-scripts

Utility submodule used by `android-urbi-framework` (and other Urbi repos).
Contains Gradle scripts for version management, changelog automation, and library publishing.

---

## Contents

| Path | Purpose |
|---|---|
| `scripts/build.gradle.kts` | Publishing tasks: version bump, changelog update, BoM publish, GitHub Release |
| `gradle/depend.gradle` | Module version definitions (`//---Module Version---//` block) |
| `gradle/libs-urbi.versions.toml` | Version catalog, including the `urbi-bom` version (date-based) |

---

## Gradle Tasks

### `update-version-lib-changelog`
**Main entry point for a full publish cycle.**

Reads `[Unreleased]` sections from all module changelogs, aggregates them into `urbisample/changelog.md`, updates the BoM version in the TOML to today's date, then delegates to `upgrade-lib-version`.

```bash
# Default (patch bump)
./gradlew update-version-lib-changelog

# Minor or major bump
./gradlew update-version-lib-changelog -Dargs=minor
./gradlew update-version-lib-changelog -Dargs=major
```

---

### `upgrade-lib-version`
Reads module changelogs for pending `[Unreleased]` entries, bumps the affected library versions in `depend.gradle`, then:

1. Writes a new entry to `bom/CHANGELOG.md` (see [BoM Changelog](#bom-changelog))
2. Creates a GitHub Release on `android-urbi-framework` (see [GitHub Release](#github-release))
3. Publishes the BoM to GitHub Packages (`bom:publishAllBom`)
4. Uploads all changed libraries (`uploadlib`)

```bash
./gradlew upgrade-lib-version           # patch bump, publishes GitHub Release
./gradlew upgrade-lib-version -Dargs=minor
./gradlew upgrade-lib-version -PskipRelease=true   # skip GitHub Release creation
```

---

### `uploadlib`
Publishes all libraries with pending changes to GitHub Packages.
For each module: replaces the `[Unreleased]` section with the new versioned entry in its `changelog.md`, then publishes the artifact.

```bash
./gradlew uploadlib
./gradlew uploadlib -Pargs=skipService   # skip copyServicesProvider
```

---

### `release-bom`
**Standalone BoM release — no lib upload required.**

For each module, looks up its current version in `depend.gradle` and reads the matching versioned section in its `changelog.md` (e.g. `## [UTL_6.26.6]`). Writes `bom/CHANGELOG.md`, publishes the BoM to GitHub Packages, and creates a GitHub Release.

Use this when:
- You uploaded one or more libraries via `uploadlib` (or any other flow) and want to cut a new BoM release.
- You want to publish a new BoM snapshot from the current state of all library versions without running a full upload cycle.

```bash
./gradlew release-bom                                  # all modules with versioned entries
./gradlew release-bom -Pmodules=utilitylib,composeds   # explicit subset
./gradlew release-bom -PskipRelease=true               # skip GitHub Release creation
```

---

### `update-version-lib`
Lighter version of `upgrade-lib-version`: bumps versions and uploads libs, but does **not** update the BoM version, write the BoM changelog, or create a GitHub Release.

---

## BoM Changelog

`bom/CHANGELOG.md` (in `android-urbi-framework`) is auto-generated on every `upgrade-lib-version` run.

**Format:**
```markdown
## [2026.05.28] 2026-05-28

### composeds — 3.4.1
- #3027 | Fix orderId nella navigazione al dettaglio storico

### utilitylib — 6.26.5
- #3100 | Updated button style
```

Each entry lists only the modules that had `[Unreleased]` changes, with their new version and the individual changelog bullets. The BoM version matches the date in `libs-urbi.versions.toml` (`yyyy.MM.dd`).

---

## GitHub Release

A GitHub Release on `urbi-mobility/android-urbi-framework` is created automatically with tag `bom-<version>` (e.g., `bom-2026.05.28`) and the same content written to `bom/CHANGELOG.md`.

**Default behaviour:** Release is created on every `upgrade-lib-version` run.

**To skip** (e.g., dry-run or CI test):
```bash
./gradlew upgrade-lib-version -PskipRelease=true
```

Requires the `gh` CLI to be authenticated in the environment (`gh auth login`).

---

## Version Management

Module versions live in `gradle/depend.gradle` between the `//---Module Version---//` and `//---End---//` markers:

```groovy
//---Module Version---//
    utilityVersion = '6.26.5'
    modelVersion   = '3.12.0'
    // ...
//---End---//
```

The BoM version is separate, in `gradle/libs-urbi.versions.toml`:
```toml
[versions]
urbi-bom = "2026.05.28"
```

It is updated automatically by `update-version-lib-changelog` to today's date before each publish cycle.
