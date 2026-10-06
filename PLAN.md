# OpenGPS Cam — Plan

An open-source, ad-free Android camera that burns a GPS label into every photo:

```
┌──────────────────────────────────────┐
│                                      │
│              (photo)                 │
│                                      │
│ ┌────────┐ 📍 Connaught Place, New    │
│ │minimap │    Delhi, DL 110001, India │
│ │  📍    │ Lat 28.6315° Long 77.2167° │
│ └────────┘ Tue, 06 Oct 2026 19:26 IST │
└──────────────────────────────────────┘
```

Semi-transparent bottom strip: minimap (left), address, lat/long, date + time + timezone.
Burned into the JPEG pixels, and the same data also goes into the EXIF fields.

## Why build it

Closed-source apps already do this (GPS Map Camera, Timestamp Camera, NoteCam, Solocator),
but they're ad-supported and request broad permissions. Open Camera (GPL) can stamp plain
text (date/GPS/address) but has no minimap and no styled strip. As far as we know, no
FOSS app has the full design. Target: F-Droid-quality, tracker-free.

## Base: Fossify Camera (fork)

- Upstream: https://github.com/FossifyOrg/Camera (GPL-3.0, so this project is GPL-3.0 too)
- Kotlin + CameraX 1.6.1, minSdk 29, compileSdk/targetSdk 36, AGP 9.4.1, Gradle 9.8.0, JVM 17
- Upstream history is kept; `upstream` remote → FossifyOrg/Camera for future merges
- Flavors: `core`, `foss`, `gplay` → use `foss` (e.g. `./gradlew assembleFossDebug`)
- App id comes from `APP_ID` in `gradle.properties` (currently `org.fossify.camera`)

### Existing hooks in the code

| File | What it does now | What we do with it |
|---|---|---|
| `helpers/SimpleLocationManager.kt` | `LocationManager` updates + best last-known fix | Extend: accuracy, staleness, fix-state callback for the UI |
| `implementations/CameraXPreview.kt` (~L585) | Builds `Metadata` (incl. `location` if `config.savePhotoVideoLocation`), calls `takePicture` | Also capture the timestamp and `StampData` at the moment of the shutter press |
| `helpers/ImageSaver.kt` (~L100) | `imageToJpegByteArray` → temp file → EXIF copy/rotate/`setGpsInfo` | **Main hook:** decode → apply rotation/flip to pixels → stamp → re-encode, then set EXIF orientation to NORMAL |
| `helpers/PhotoProcessor.kt` | Legacy; never instantiated (only its `MediaSavedListener` interface is used) | Leave it alone |
| `helpers/Config.kt` + `SettingsActivity.kt` | Preferences | New stamp settings |

## Architecture (new code)

```
CameraX capture ──► JPEG bytes ──► PhotoStamper ──► MediaStore save (+ EXIF)
                                       ▲
LocationProvider ─► AddressResolver ─► StampData {lat, lng, accuracy, address, time, tz}
       │                               ▲
       └────────► MinimapRenderer ─────┘ (bitmap)
```

Put new code under `org.fossify.camera.stamp` (or after the rename, `org.opengpscam.stamp`):

- **StampData**: immutable snapshot taken at the shutter press: lat, lng, accuracy,
  altitude?, address?, `ZonedDateTime`, isStale.
- **LocationProvider**: builds on `SimpleLocationManager`. Starts when the camera opens, so a fix
  is warm before you tap the shutter. Plain `LocationManager` (no Play Services) to stay FOSS.
- **AddressResolver**: `android.location.Geocoder` (async `getFromLocation` with a listener on
  API 33+). Cache by coordinates rounded to ~4 decimals. Offline → coordinates only.
- **MinimapRenderer**: an offscreen map bitmap centred on the fix, with a pin.
  - Recommended: osmdroid or MapLibre Native, using OSM raster tiles, rendered offscreen.
    No API key needed. Follow the OSM tile usage policy: real User-Agent, disk cache, no bulk
    prefetch. Show "© OpenStreetMap" attribution in the strip or in the About screen.
  - Fallback when there are no tiles: a simple card with a grid and pin.
  - Render it asynchronously as soon as the fix changes, so the shutter never waits on network.
- **PhotoStamper**: pure function `(Bitmap, StampData, minimap: Bitmap?, StampStyle) → Bitmap`.
  Size the strip, fonts and minimap relative to the image's short edge, so it looks the same
  at 12 MP and 50 MP. Canvas + StaticLayout for text wrapping.
- **StampOverlayView**: a live preview of the same strip over the viewfinder (WYSIWYG).

## Phases

0. **Setup** ✅ partly done
   - [x] Fork Fossify Camera, keeping its history
   - [x] Local SDK: platforms 34+36, build-tools 34+36, JDK 17
   - [x] Upstream history merged into this repo (`upstream` remote)
   - [ ] `./gradlew assembleFossDebug` builds the unmodified app — **blocked**: cloud network policy denies `www.jitpack.io` (needed for `org.fossify:commons`); Maven Central also rate-limits (429) occasionally. Android SDK is at `/opt/android-sdk`, JDK 21 works as far as plugin resolution goes
   - [~] Rebrand: `APP_ID=org.opengpscam` and app name done (Kotlin namespace stays `org.fossify.camera` for now); icon and README still to do
1. **Location**: permission flow, extended LocationProvider, accuracy and "waiting for GPS"
   indicator, AddressResolver with cache
2. **Stamping (text only)**: StampData snapshot at the shutter press, PhotoStamper hooked
   into `ImageSaver`. Handle rotation and front-camera mirroring correctly, and set EXIF
   orientation to NORMAL after rotating the pixels. Test portrait/landscape × front/back × resolutions.
3. **Minimap**: MinimapRenderer + tile cache + fallback card, composited into the strip
4. **Live overlay** on the viewfinder
5. **Settings**: toggle each field, 12h/24h, decimal/DMS coordinates, strip position
   (top/bottom) and opacity, map on/off, keep an unstamped original too, stamp on/off
6. **Polish & release**
   - Never block the shutter: if the fix is old, use the last known one and mark it stale
   - Stamp on a background thread
   - Memory: a 50 MP ARGB bitmap is about 200 MB → `inSampleSize` or tiled processing on low-RAM devices
   - Tests: unit tests (formatting, layout maths) and golden-image tests for PhotoStamper
   - GPL notices, OSM attribution, signed release APK, F-Droid metadata (`fastlane/` exists)

**Out of scope for v1:** video stamping (every frame has to be re-encoded), custom templates, iOS.

## Risks

- Indoor / poor GPS: show accuracy (±m) on the label, and fall back to the last known fix
- No network: cached tiles, then the fallback card. No address: coordinates only
- Privacy: location is read only while the camera is open; the EXIF location can be turned off
- AGP 9 / Gradle 9 are new: if the build breaks on JDK 17, check the upstream CI JDK version
  (`.github/workflows`)

## Dev environment notes (cloud or local)

- Needs the Android SDK with `platforms;android-36` and `build-tools;36.0.0`. Point to it with
  `local.properties` (`sdk.dir=...`, gitignored) or `ANDROID_HOME`.
  - Cloud box without an SDK: download commandlinetools-linux from
    https://developer.android.com/studio#command-line-tools-only, then
    `yes | sdkmanager --licenses && sdkmanager "platform-tools" "platforms;android-36" "build-tools;36.0.0"`
- Build: `./gradlew assembleFossDebug` → `app/build/outputs/apk/foss/debug/`
- Lint/static checks: `./gradlew detekt lintFossDebug`
- Testing camera and GPS needs a real phone (`adb install -r <apk>`); emulator GPS/camera is a poor stand-in.
