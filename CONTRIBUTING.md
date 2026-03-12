# Contributing to airport-data

## Development Setup

1. Clone the repository:
   ```bash
   git clone https://github.com/aashishvanand/airport-data-kotlin.git
   cd airport-data-kotlin
   ```

2. Build the project:
   ```bash
   ./gradlew build
   ```

3. Run tests:
   ```bash
   ./gradlew test
   ```

## Branch Strategy

| Branch | Purpose |
|---|---|
| `main` | Active development. All feature work and bug fixes go here. |
| `release` | Release branch. Merges from `main` when ready to publish a new version. |

- Pushes and PRs to `main` trigger CI (build + test) only.
- Pushing a version tag (`v*`) to `release` triggers publishing to Maven Central and GitHub Packages.

## Making Changes

1. Create a feature branch from `main`:
   ```bash
   git checkout main
   git pull origin main
   git checkout -b feature/your-feature-name
   ```

2. Make your changes and ensure tests pass:
   ```bash
   ./gradlew build
   ```

3. Open a pull request against `main`.

## Releasing a New Version

### 1. Update the version number

In `build.gradle.kts`, update the `version` field:

```kotlin
version = "1.1.0"  // update this
```

Commit and push to `main`:

```bash
git add build.gradle.kts
git commit -m "Bump version to 1.1.0"
git push origin main
```

### 2. Merge into the release branch

```bash
git checkout release
git pull origin release
git merge main
```

### 3. Tag and push

Create an annotated tag matching the version and push both the branch and tag:

```bash
git tag -a v1.1.0 -m "Release v1.1.0: <brief description>"
git push origin release --tags
```

### 4. Monitor the release

The publish workflow will automatically:
- Run tests
- Upload signed artifacts to Maven Central (via OSSRH Staging API)
- Finalize the deployment with automatic publishing
- Publish to GitHub Packages

Monitor progress at: https://github.com/aashishvanand/airport-data-kotlin/actions

Once the workflow completes, verify the deployment at: https://central.sonatype.com/publishing/deployments

The artifact typically becomes resolvable via `mavenCentral()` within 30 minutes.

### 5. Switch back to main

```bash
git checkout main
```

## Version Guidelines

This project follows [Semantic Versioning](https://semver.org/):

- **PATCH** (1.0.x): Bug fixes, data updates
- **MINOR** (1.x.0): New features, backward-compatible API additions
- **MAJOR** (x.0.0): Breaking API changes

## Project Structure

```
src/main/kotlin/dev/airportdata/
  Airport.kt              - Airport data class and serializers
  AirportData.kt          - Main library API
  AirportDataException.kt - Custom exception class
  AirportLinks.kt         - External links data class
  AirportStats.kt         - Statistics and distance matrix data classes

src/test/kotlin/dev/airportdata/
  AirportDataTest.kt      - Test suite

data/
  airports.json           - Raw airport dataset (~11 MB)
```

The `airports.json` file is gzip-compressed at build time into `src/main/resources/airports.json.gz` and embedded in the JAR.
