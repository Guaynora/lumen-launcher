# Lumen

A lightweight Android TV launcher, built for a TCL TV running Android TV 14 with 1 GB of RAM.

- App grid (6 columns) with banners, reorder and hide via long-press on OK
- Collapsible inputs row (TV, HDMI, composite) and an input picker shortcut
- Free RAM indicator and a "free RAM" button that closes background apps
- Optional background image picked from the TV's photo picker
- Accessibility service that routes the remote's Home button to Lumen

The app UI is in Spanish.

## Build and install

Builds without Gradle using `aapt2`, `javac`, `d8` and `apksigner`.
`build.sh` expects a JDK and the Android SDK (platform 34, build-tools 34.0.0) under `~/.local/android-build`.

```
./build.sh
adb connect <TV-IP>:5555
adb install -r lumen.apk
```

## Make Lumen the home screen

On Google TV the stock launcher wins over third-party home apps, so it has to be disabled together with
the setup wizard (otherwise Home opens a recovery screen):

```
adb shell pm disable-user --user 0 com.google.android.apps.tv.launcherx
adb shell pm disable-user --user 0 com.google.android.tungsten.setupwraith
adb shell settings put secure enabled_accessibility_services com.lumen.launcher/com.lumen.launcher.HomeService
adb shell settings put secure accessibility_enabled 1
```

## Background image

Use **Sistema → Fondo** on the TV, or push one from a PC:

```
adb push photo.jpg /sdcard/Android/data/com.lumen.launcher/files/fondo.jpg   # set
adb shell rm /sdcard/Android/data/com.lumen.launcher/files/fondo.jpg         # remove
```

A portrait image about 720 px tall works best. It is shown on the right side.

## Go back to Google TV

1. On the TV, turn on **Network debugging** (Settings → System → Developer options).
2. From a PC:

```
adb connect <TV-IP>:5555
adb shell pm enable --user 0 com.google.android.apps.tv.launcherx
adb shell pm enable --user 0 com.google.android.tungsten.setupwraith
adb shell settings put secure enabled_accessibility_services null
adb uninstall com.lumen.launcher
```

3. Press Home on the remote.

A factory reset also restores Google TV, but it erases all apps and accounts.

## Reinstall removed apps

- Preinstalled apps removed with `pm uninstall --user 0` (e.g. Prime Video):
  `adb shell cmd package install-existing com.amazon.amazonvideo.livingroom`
- Other apps: reinstall from the Play Store.
