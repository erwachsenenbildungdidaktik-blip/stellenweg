# Stellenweg – Projekt

- **iPhone / Browser:** https://erwachsenenbildungdidaktik-blip.github.io/stellenweg/ (in Safari öffnen, Teilen → Zum Home-Bildschirm)
- **Android:** [Stellenweg.apk herunterladen](https://erwachsenenbildungdidaktik-blip.github.io/stellenweg/download/Stellenweg.apk) (Installation aus unbekannten Quellen erlauben)

Eine App, zwei Varianten aus derselben Web-App:

| Variante | Ordner | Für |
|---|---|---|
| Web-App | `web/` | iPhone (Home-Bildschirm), Browser, Moodle, GitHub Pages |
| Android-App (APK) | `android/` | Android; lädt `web/` offline aus der APK und ergänzt native Funktionen |

## Was die APK zusätzlich kann

Die Hülle stellt der Web-App `window.AndroidApp` bereit (`android/app/src/main/java/ch/stellenweg/app/MainActivity.java`). Die Web-App erkennt das und nutzt dann:

- **Seitenleiste:** Das Plus listet die installierten Apps mit echten Symbolen. Ein Tipp öffnet die App als schwebendes Fenster, im geteilten Bildschirm oder ganz (Einstellung im Plus-Blatt). Schwebend braucht ein Handy mit Freiform-Fenstern (ColorOS ja); sonst fällt es auf geteilt bzw. ganz zurück.
- **Mitteilungen** exakt zur Uhrzeit über den AlarmManager, auch nach einem Neustart.
- **Teilen-Ziel:** Text aus jeder App (Teilen → Stellenweg) landet im Notizfenster.
- **PDF senden und speichern**, Dokumente mit der passenden App **öffnen** (`openFile`), Kamera beim Hochladen, Zwischenablage, Vibration.
- **Griff der Seitenleiste** ist von der Zurück-Geste am rechten Rand ausgenommen (`setGestureExclusion`, Android 10+), damit ein Wisch die Leiste öffnet statt die App zu verlassen.

In der Web-App (iPhone) gibt es dafür das **Notizfenster** (Seitenleiste → Notizen): Alles markieren, Kopieren, Einfügen, Teilen, Leeren; Einfügen geht auch durch langes Drücken ins Feld.

## Updates

- **Web-App / iPhone:** Beim Start mit Internet lädt der Service Worker automatisch die neueste Version; danach erscheint kurz „Stellenweg wurde auf Version … aktualisiert“.
- **Android-App:** Beim Start vergleicht die App ihre `versionCode` mit `web/version.json` auf GitHub Pages. Ist eine neuere APK da, erscheint „Neue Version“ mit „Jetzt aktualisieren“: Die App lädt die APK und startet die Installation. Android verlangt bei Apps ausserhalb des Play Stores immer eine Bestätigung; beim ersten Mal zusätzlich „Apps aus dieser Quelle erlauben“. Daten bleiben erhalten (gleicher Schlüssel). „Später“ fragt einen Tag lang nicht mehr.
- **Neue Version veröffentlichen:** in `android/app/build.gradle` `versionCode` (+1) und `versionName` erhöhen, „Was ist neu“ in `android/release-notes.txt`, dann `APK-bauen.cmd` (schreibt auch `web/version.json`), committen und pushen.
- Version 1.0 hatte noch kein Auto-Update: Wer sie installiert hat, muss 1.1 einmal von Hand installieren.

## APK bauen

Doppelklick auf `APK-bauen.cmd`. Ergebnis: `web/download/Stellenweg.apk`.

Werkzeuge liegen portabel in `tools/` (nicht in Git): Java 17 (Temurin), Android SDK (Plattform 35, Build-Tools 35), Gradle 8.10.2. Vor einem Update `versionCode` und `versionName` in `android/app/build.gradle` erhöhen.

## Signatur – unbedingt sichern

Updates lassen sich nur über die alte Version installieren, wenn die APK mit **demselben Schlüssel** signiert ist. Der Schlüssel liegt in `tools/stellenweg-release.jks`, das Passwort in `android/keystore.properties`. Beides ist bewusst nicht in Git. **Beide Dateien an einem sicheren Ort sichern** (z.B. verschlüsselter USB-Stick). Ohne sie muss die App bei allen deinstalliert und neu installiert werden (Daten gehen dabei verloren, wenn keine Sicherung gemacht wurde).

## Testen

- Web-App: `python -m http.server 8000` im Ordner `web`, dann http://localhost:8000
- APK auf dem Handy: Datei aufs Handy kopieren und öffnen (Installation aus unbekannten Quellen erlauben) oder mit USB-Debugging: `tools\android-sdk\platform-tools\adb install -r web\download\Stellenweg.apk`

## Fremdkomponenten

- MediaPipe Tasks Vision 0.10.14 und Modell selfie_segmenter (Google, Apache License 2.0), in `web/vendor/mediapipe`, für das Freistellen des Porträts im Lebenslauf-Assistenten. Details in `web/vendor/mediapipe/NOTICE.txt`.
