# OpenGPS Cam

[![Download latest APK](https://img.shields.io/github/v/release/ratan00/OpenGPSCam?label=Download%20APK&logo=android&logoColor=white&color=3DDC84&style=for-the-badge)](https://github.com/ratan00/OpenGPSCam/releases/latest)
[![Downloads](https://img.shields.io/github/downloads/ratan00/OpenGPSCam/total?style=for-the-badge&color=blue)](https://github.com/ratan00/OpenGPSCam/releases)

An open-source, ad-free Android camera that burns a GPS label into every photo: a minimap, the
address, latitude/longitude and the date, time and timezone, on a semi-transparent strip. The same
text is written into the JPEG's EXIF description.

- Live preview of the label over the viewfinder, drawn by the same code as the saved photo
- Never blocks the shutter: the location, address and map are prepared in the background, and an
  old fix is used and marked "(last known)"
- Offline-friendly: no address means coordinates only, no map tiles means a simple grid card
- Settings: each field on/off, 12/24-hour time, decimal or DMS coordinates, strip at top or bottom,
  opacity, minimap on/off, and an option to keep an unstamped original too
- No ads, no analytics, no Play Services. Location is read only while the camera is open

## Network use

The only network access is fetching a handful of OpenStreetMap tiles around your position for the
minimap (identifying User-Agent, on-disk cache, no prefetching, in line with the
[OSM tile usage policy](https://operations.osmfoundation.org/policies/tiles/)). Turn the minimap off
in settings and the app never goes online. Addresses come from the system `Geocoder`.

Map data © [OpenStreetMap contributors](https://www.openstreetmap.org/copyright) (ODbL). The strip
carries the "© OpenStreetMap" credit whenever a real map is shown.

## Building

```
echo "sdk.dir=/path/to/android-sdk" > local.properties   # platforms;android-36, build-tools;36.0.0
./gradlew assembleFossDebug          # app/build/outputs/apk/foss/debug/
./gradlew detekt lintFossDebug testFossDebugUnitTest
```

Camera and GPS need a real phone: `adb install -r <apk>`. See [PLAN.md](PLAN.md) for the design.

## Credits and license

A fork of [Fossify Camera](https://github.com/FossifyOrg/Camera). Licensed under the
[GNU GPL v3.0](LICENSE); upstream history is kept, and the `upstream` remote can be used to merge
future changes.
