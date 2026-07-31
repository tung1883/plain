# Plain

Go back and time and turn your smartphones into bricks. Together we will stopp doomscrolling

## Features

- **Plain home screen** — plain and simple home UI, apps not even getting their own icons
- **Flagged-app gating** — countdown before you can use those gross addictive apps
- **Grayscale** - no more color, everyone is colorblind now

And when you ever want to change the settings, you need to wait and solve some math problems (we all know you are solving no math)

## Requirements

- Android 8.0+
- JDK 17

## Building

```
./gradlew assembleDebug
```

The debug APK is produced at `app/build/outputs/apk/debug/app-debug.apk`.

### Grayscale permission

Grayscale needs `android.permission.WRITE_SECURE_SETTINGS`, which must be granted via `adb`:

```
adb shell pm grant com.plainphone.app android.permission.WRITE_SECURE_SETTINGS
```