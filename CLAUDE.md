# presse-app

Native Android-App (Kotlin, Jetpack Compose, Material 3) als Ersatz für den
Scraping-Ansatz des `paywall-bot`-NAS-Containers: übernimmt nur den Login bei
VÖBB/Genios/Münzinger und zeigt danach die echte Original-Seite live in einer
WebView – kein Download, kein eigener Nachbau der Inhalte.

Details/Architektur/Sicherheitsdesign: siehe Plan-Datei aus der Entstehungs-
Session (`robust-gliding-stearns.md`) bzw. Code-Kommentare in den einzelnen
Dateien, insbesondere `LoginAutomator.kt` (Portierung der Login-Logik aus
`paywall-bot/bot.py`) und `SecureCredentialStore.kt` (DataStore + Tink +
Android Keystore für die verschlüsselte Ablage der Zugangsdaten).

## Build

Voraussetzung: JDK 17, Android SDK (cmdline-tools reichen, kein Android
Studio nötig). `ANDROID_HOME`/`local.properties` müssen auf eine SDK-
Installation mit `platforms;android-36` + `build-tools;36.0.0` (oder neuer)
zeigen.

```
./gradlew assembleDebug
```

APK liegt danach unter `app/build/outputs/apk/debug/app-debug.apk`.
Debug-Signatur reicht für den dauerhaften persönlichen Sideload-Gebrauch,
kein separates Release-Signing nötig.

## Installation aufs Handy

Bevorzugt über `adb` per WLAN (kein USB/Proxmox-Passthrough nötig):

1. Am Handy: Einstellungen → Entwickleroptionen → "Kabelloses Debugging" an,
   "Gerät mit Pairing-Code koppeln" antippen.
2. `adb pair <IP>:<Port> <Pairing-Code>`, danach `adb connect <IP>:<Port>`.
3. `adb install -r app/build/outputs/apk/debug/app-debug.apk`.

Alternativ (kein `adb` nötig): APK über einen kurzlebigen
`python3 -m http.server` im LAN anbieten und auf dem Handy direkt
herunterladen/antippen (einmalig "Installation aus unbekannten Quellen"
für den Browser erlauben).

## Quellen-Einstiegspunkte

Siehe `data/Sources.kt` – Spiegel über Münzinger, c't/IX/FAZ über Genios.
FAZ hat (Stand Ersteinrichtung) noch keinen bestätigten `toc_list`-Code,
läuft vorerst über die generische Genios-Startseite.
