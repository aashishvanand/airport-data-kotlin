---
name: update-airport-data
description: Sync data/airports.json from airport-data-js into airport-data-kotlin, regenerate derived data files, check dependencies, bump the patch version and release. Use when the user says the JS library has new airport data, asks to update/sync airports, or asks for a data release.
---

# Update airport data (airport-data-kotlin)

`airport-data-js` is the source of truth for the dataset. This repo ships a copy of it.
Work from the repo root (`/Users/aashishvanand/Code/airport-data-kotlin`).

## 1. Get the new data

```bash
git checkout main && git pull --ff-only
git -C ../airport-data-js fetch origin
git -C ../airport-data-js show origin/main:data/airports.json > data/airports.json
git diff --stat data/airports.json   # nothing changed? stop, there is nothing to release
```

## 2. Scan field types before touching code

Upstream sometimes changes value types (for example, `""` became `null` for missing
`elevation_ft` / `runway_length` in JS 4.0.0). Compare old against new for every field:

```bash
git show HEAD:data/airports.json > /tmp/old_airports.json
python3 - <<'EOF'
import json
from collections import Counter
o = json.load(open('/tmp/old_airports.json')); n = json.load(open('data/airports.json'))
print('count', len(o), '->', len(n))
for f in n[0]:
    co = Counter(type(a.get(f)).__name__ for a in o); cn = Counter(type(a.get(f)).__name__ for a in n)
    if co.keys() != cn.keys():
        print(f, dict(co), '->', dict(cn))
old = {(a['iata'], a['icao']) for a in o}
print('added', [(a['iata'], a['icao'], a['airport']) for a in n if (a['iata'], a['icao']) not in old])
EOF
```

If a field gains a new type (`NoneType`, `str` in a numeric field, and so on), check that the decoder below handles it
and add a test for it. Also read the top of `../airport-data-js/CHANGELOG.md` for data fixes and breaking type changes.

Decoder: `IntOrEmptySerializer` / `DoubleOrEmptySerializer` in `src/main/kotlin/dev/airportdata/Airport.kt`.
These handle numbers, `""`, and `null` (`JsonNull` is a `JsonPrimitive`). `latitude` / `longitude` are non-null `Double`.

## 3. Regenerate derived files

Nothing to commit here. The `compressAirportData` Gradle task gzips `data/airports.json` into
`src/main/resources/airports.json.gz` at build time, and that file is untracked.

## 4. Check for dependency updates

The build uses Kotlin 1.9.24 and Gradle 8.7. `kotlinx-serialization-json` 1.6.3 is the last release that supports Kotlin 1.9,
so only take compatible patch bumps here. A Kotlin 2.x / Gradle 9 upgrade is separate work, because it affects the signing and publish config.
Check `.github/workflows/*.yml` action versions too.

## 5. Bump the version (patch for data-only updates)

Bump `version = "X.Y.Z"` in `build.gradle.kts`.

## 6. Verify locally (same checks as CI)

```bash
# Gradle 8.7 can't run on JDK 25. Use JDK 17 or 21. The build's toolchain is 21.
JAVA_HOME=/opt/homebrew/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home ./gradlew build
grep -ho 'tests="[0-9]*".*errors="[0-9]*"' build/test-results/test/*.xml
```

If a test fails, confirm the failure comes from a real data change (new airport count, corrected
coordinates, and so on) before you update the expected value.

## 7. Commit and push main, then wait for CI

Stage explicit paths only. Never use `git add .`, because there are untracked local files.

```bash
git add build.gradle.kts data/airports.json
git commit -m "feat: sync airport data with airport-data-js and bump version to X.Y.Z"
git push origin main
gh run list --branch main --limit 1        # then: gh run watch <id> --exit-status
```

## 8. Release (only after CI on main is green; publishing is irreversible)

Push a `v*` tag on `release` to publish to Maven Central and GitHub Packages:
```bash
git checkout release && git pull --ff-only && git merge --ff-only main
git tag -a vX.Y.Z -m "Release vX.Y.Z: <summary>"
git push origin release --tags
git checkout main
```
Check https://repo1.maven.org/maven2/dev/airportdata/airport-data/maven-metadata.xml. It can take about 30 minutes to update.
