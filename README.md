# RouteLab GPS Simulator

Android project for testing simulated location routes on Samsung/Android devices.

## Project delivery

This repository includes a GitHub Actions workflow for building and packaging the Android debug APK. The workflow requires the Android Studio project files: Kotlin sources, Gradle build files, Gradle wrapper, manifest and tests.

## Build locally

Use Android Studio with JDK 17 and Android SDK Platform 35 installed. Run:

```bash
./gradlew test assembleDebug
```

On Windows, use `gradlew.bat test assembleDebug`.

The APK is expected at `app/build/outputs/apk/debug/app-debug.apk` after a successful build.

## Enable a mock location on Samsung

1. Open Settings → About phone → Software information.
2. Tap Build number seven times and confirm your device PIN if asked.
3. Open Settings → Developer options.
4. Select RouteLab as the mock location app after installing it.
5. Use mock locations for legitimate testing. Android marks simulated locations, and individual apps may detect or reject them.

## GitHub Actions

The workflow in `.github/workflows/android-release.yml` runs on pushes to `main`, version tags such as `v0.1.0`, and manual dispatch. Version-tag builds attach the generated APK to a GitHub Release if the project builds successfully.
