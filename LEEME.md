# Lumen

Launcher ligero para la TV TCL (Android TV 14, 1 GB de RAM).

## Compilar e instalar

```
./build.sh
adb connect <IP-de-la-TV>:5555
adb install -r lumen.apk
```

Las herramientas (JDK y SDK de Android) están en `~/.local/android-build`.

## Fondo de pantalla

```
adb push foto.jpg /sdcard/Android/data/com.lumen.launcher/files/fondo.jpg   # poner
adb shell rm /sdcard/Android/data/com.lumen.launcher/files/fondo.jpg         # quitar
```

Conviene una imagen vertical de unos 720 px de alto. Se muestra a la derecha.

## Volver al launcher de Google TV

1. Activa **Depuración por red** en la TV (Ajustes → Sistema → Opciones para desarrolladores).
2. Desde la PC:

```
adb connect <IP-de-la-TV>:5555
adb shell pm enable --user 0 com.google.android.apps.tv.launcherx
adb shell pm enable --user 0 com.google.android.tungsten.setupwraith
adb shell settings put secure enabled_accessibility_services null
adb uninstall com.lumen.launcher
```

3. Presiona Inicio en el control.

Si no tienes la PC a mano: **restablecer la TV de fábrica** también vuelve a Google TV
(pero borra todas las apps y cuentas).

## Reinstalar apps quitadas

- Prime Video (venía de fábrica): `adb shell cmd package install-existing com.amazon.amazonvideo.livingroom`
- Disney+ y HBO Max: desde la Play Store.
