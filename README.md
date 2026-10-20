# RouteLab GPS Simulator

Android project for testing simulated location routes on Samsung/Android devices.

## Status

This repository currently needs the Android Studio project files (Kotlin sources, Gradle build files, wrapper, manifest, and tests) before the CI workflow can produce an APK. The release workflow is configured to validate the project, run tests, build a debug APK, publish it as a workflow artifact, and attach it to a GitHub Release when a `v*` tag is pushed.

## Build locally

Open the project root in Android Studio with JDK 17 and Android SDK Platform 35 installed. Once the complete Gradle project is present, run:

```bash
./gradlew test assembleDebug
```

The debug APK should be created at:

```
app/build/outputs/apk/debug/app-debug.apk
```

On Windows, use `gradlew.bat test assembleDebug`.

## Enable a mock location on Samsung

1. Open Settings → About phone → Software information.
2. Tap Build number seven times and confirm your device PIN if asked.
3. Open Settings → Developer options.
4. Select RouteLab as the mock location app, after installing it.
5. Use mock locations only for legitimate testing. Android may label simulated locations, and individual apps may detect or reject them.

## GitHub Actions

The workflow in `.github/workflows/android-release.yml` runs on pushes to `main`, version tags such as `v0.1.0`, and manual dispatch. A version-tag build publishes a GitHub Release with the generated APK, but only if the project builds successfully.
