# NaviFlux · Route Studio

NaviFlux is an Android app for building and previewing routes on an OpenStreetMap map.

## Version 2.0

- Interactive OpenStreetMap view.
- Tap the map to choose the start and destination.
- Start/end coordinates and straight-line distance.
- Adjustable in-app marker playback with pause and reset.
- Dark neon interface and a custom adaptive app icon.
- No device-location permission and no system-wide mock-location provider.

**Route note:** the preview connects the chosen points with a straight line. It is a visual route lab, not turn-by-turn road navigation. The animated marker moves only inside NaviFlux and does not change the location seen by other apps.

Map data © [OpenStreetMap contributors](https://www.openstreetmap.org/copyright). Map tiles require an internet connection.

## Build locally

Use Android Studio with JDK 17 and Android SDK Platform 35 installed. Run:

```bash
./gradlew test assembleDebug
```

The APK is generated at `app/build/outputs/apk/debug/app-debug.apk`.

## GitHub Actions and releases

Pushing a version branch such as `release/v2.0.0` builds and tests the app, uploads the APK artifact, and publishes a matching GitHub Release (`v2.0.0`) when the build succeeds. Pushing a version tag like `v2.1.0` also builds and publishes a release.
