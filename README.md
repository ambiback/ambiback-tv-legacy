# AmbiBack TV (Legacy)

**English** · [Deutsch](README.de.md)

Former Android TV app for AmbiBack. It captures the screen content and sends the colors over the network to AmbiBack devices.

> **Archived:** This repository is no longer developed and is kept for documentation. To set up and control current firmware, use [AmbiBack Manager](https://github.com/ambiback/ambiback-manager).

AmbiBack devices have not been on sale for around five years. More about the project at [ambiback.de](https://www.ambiback.de/).

## Features

- Screen capture through the Android MediaProjection API
- Find devices on the local network over UDP
- Send color data to selected AmbiBack devices
- Device and app settings

## Building

The project uses its 2020/2021 state: Gradle 6.5, Android Gradle Plugin 4.1.2, Kotlin 1.3.31, Android API 28 and the discontinued JCenter repository. This state does not run on current JDKs. A build needs an older JDK (8 or 11) and possibly changes to the dependencies.

```sh
./gradlew assembleDebug
```

This state was not rebuilt or tested for this publication.

## Security note

The app and the devices communicate unencrypted and without authentication. Only use them on a trusted network.

## License

Copyright (C) 2020-2026 Andre Lorbach

This program is free software under the [GNU General Public License version 3](LICENSE). It comes with no warranty. Bundled libraries keep their own licenses.
