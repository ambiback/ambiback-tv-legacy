# AmbiBack TV (Legacy)

Frühere Android-TV-App von AmbiBack. Sie nimmt den Bildschirminhalt auf und sendet die Farben über das Netzwerk an AmbiBack-Geräte.

> **Archiviert:** Dieses Repository wird nicht mehr weiterentwickelt und dient der Dokumentation. Für die Einrichtung und Steuerung aktueller Firmware gibt es den [AmbiBack Manager](https://github.com/ambiback/ambiback-manager).

Die AmbiBack-Geräte werden seit rund fünf Jahren nicht mehr verkauft. Mehr zum Projekt steht auf [ambiback.de](https://www.ambiback.de/).

## Funktionen

- Bildschirmaufnahme über die Android-MediaProjection-API
- Geräte im lokalen Netzwerk per UDP finden
- Farbdaten an ausgewählte AmbiBack-Geräte senden
- Geräte- und App-Einstellungen

## Bauen

Das Projekt nutzt den Stand von 2020/2021: Gradle 6.5, Android Gradle Plugin 4.1.2, Kotlin 1.3.31, Android API 28 und das abgeschaltete Repository JCenter. Mit aktuellen JDKs läuft dieser Stand nicht. Ein Build braucht ein älteres JDK (8 oder 11) und möglicherweise Anpassungen an den Abhängigkeiten.

```sh
./gradlew assembleDebug
```

Der Stand wurde für diese Veröffentlichung nicht neu gebaut oder getestet.

## Sicherheitshinweis

Die App und die Geräte kommunizieren unverschlüsselt und ohne Anmeldung. Nutze sie nur in einem vertrauenswürdigen Netz.

## Lizenz

Copyright (C) 2020-2026 André Lorbach

Dieses Programm ist freie Software und steht unter der [GNU General Public License Version 3](LICENSE). Es wird ohne jede Gewährleistung bereitgestellt. Eingebundene Bibliotheken behalten ihre eigenen Lizenzen.
