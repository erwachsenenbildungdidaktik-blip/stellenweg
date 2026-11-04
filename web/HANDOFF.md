# Übergabe an Claude Code

Stand 4. November 2026. Prototyp aus einem Chat mit Hugo Rohrbacher (Fachkraft Bewerbung, CSP Biel). Diese Datei erklärt, was die App ist, wie sie gebaut ist und was als Nächstes kommt.

## Auftrag für diese Session

1. Neues öffentliches Repo `stellenweg` unter dem Account `erwachsenenbildungdidaktik-blip` anlegen (gleich aufgebaut wie `bewerbung-pdf`).
2. Diesen Ordner pushen, GitHub Pages auf `main` / Root aktivieren.
3. Prüfen, dass `https://erwachsenenbildungdidaktik-blip.github.io/stellenweg/` lädt und offline funktioniert (Service Worker).
4. Danach die offenen Punkte unten mit Hugo besprechen, nicht ungefragt umsetzen.

Beispiel:

```bash
gh repo create erwachsenenbildungdidaktik-blip/stellenweg --public --source . --push
gh api -X POST repos/erwachsenenbildungdidaktik-blip/stellenweg/pages -f "source[branch]=main" -f "source[path]=/"
```

## Was die App ist

Ersetzt ein Arbeitsbuch für Stellensuchende in der AMM (BIN-Programm, CSP Biel). Zielgruppe: RAV-Kundinnen und -Kunden, teils wenig PC-erfahren. Läuft auf Android, iOS (Home-Bildschirm-Web-App) und später als HTML in Moodle.

Tabs (Reihenfolge der Kursmodule):

| Tab | Kursmodul | Stand |
|---|---|---|
| Kompetenzen & Ressourcen | A1.II, A1.III | Gerüst, Import aus „Kompetenz-Spiegel“ (JSON) geplant |
| Strategie & Aktionsplan | A1.IV, A1.V | fertig: Bewerbungs-Tracker, Aktionsplan, Strategie |
| Dossier & Portfolio | A2 | fertig: Dokumentstapel, Upload, Brief-Assistent, beco-Check |
| Netzwerk | A3–A5 | Gerüst, soll Hugos A5-Tool „Mein Netzwerk“ übernehmen |
| RAV & ALK | – | fertig: Arbeitsbemühungen, Formular 716.007, ALK-Monatsordner, Krankheit |

## Architektur

- **Eine Datei** `index.html` mit CSS und JS inline, keine Build-Kette. Bibliotheken lokal in `vendor/`, werden bei Bedarf nachgeladen (`loadScript`).
- **Speicher:** IndexedDB `stellenweg`, Stores `kv` (Key `state` = ganzer Zustand als JSON) und `blobs` (Dokumente, Key = Dokument-ID). Fallback Arbeitsspeicher, falls IndexedDB fehlt. Migration aus altem `localStorage`-Key `stellenweg.v1` ist eingebaut.
- **Sicherung:** ZIP mit `stellenweg.json` und `dokumente/<id>`. Erinnerung nach 7 Tagen ohne Sicherung. Hintergrund: Safari löscht Speicher von Websites nach 7 Tagen ohne Nutzung.
- **Formular 716.007** (Nachweis der persönlichen Arbeitsbemühungen) liegt als Base64 im `<script id="form716007">`. Ausfüllen mit pdf-lib, 14 Zeilen pro Blatt, weitere Blätter automatisch, danach `flatten()`. Einige Felder haben keine Default Appearance, darum der Fallback in `buildNachweis`.
- **Brief-Assistent** ist aus `bewerbung-pdf/index.html` portiert (Layout, eigener PDF-Writer mit Helvetica/WinAnsi, Text aufräumen, Unterschrift mit Hintergrundentfernung). Unterschied: Zustand liegt in `S.profil` (einmalige Angaben, Unterschrift) und `S.brief` (aktueller Entwurf).

## Datenmodell (`S`)

- `bewerbungen[]`: `id, firma, adresse, kontakt, tel, stelle, datum, kanal, art (brieflich|persoenlich|telefonisch), pensum (voll|teil), zuweisung, status (arbeit|versendet|gespraech|absage|zusage), absagegrund, naechster, beispiel?`
- `aufgaben[]`, `strategie{ziele[], region, pensum, verdeckt, botschaft}`, `ziele{woche, monat}`
- `docs[]`: Metadaten, Blob in IndexedDB. Dossier-Dokumente haben `kat`, ALK/RAV-Dokumente zusätzlich `bereich:'alk'` und `slot (avp|zv|arzt|dauer|nachweis)`, `monat`, `fallId`.
- `alk{monate{YYYY-MM:{zv, eingereicht{avp,zv,nachweis}}}, faelle[], kasse, tel, rahmenfrist}`
- `profil{meName, meStrasse, mePlzOrt, meTel, meMail, bar, barColor, barText, sigRaw, sig, sigClean, sigRot}`
- `check{}` (beco-Checkliste), `meta{lastBackup, docsSeeded}`
- Beispieldaten tragen `beispiel:true` und lassen sich im Tracker mit einem Knopf entfernen.

## Fachliche Regeln (mit Quelle)

- Arbeitsbemühungen: Abgabe ab dem 22., spätestens 5. des Folgemonats (Kanton Bern, Angabe Hugo; Formular 716.007 bestätigt den 5.).
- Krankheit (Merkblatt Krankheit, AVA Kanton Bern): sofort beim RAV melden, Arztzeugnis ab dem 4. Tag an die Kasse, Krankentage in die AvP, höchstens 30 Kalendertage am Stück, höchstens 44 Taggelder pro Rahmenfrist. Der Zähler in der App ist eine Schätzung (Werktage Mo–Fr, ohne Feiertage).
- Dossier-Check: Checkliste für Bewerbungsdossiers, beco Berner Wirtschaft.
- AHV-Nummer wird nie gespeichert, nur beim Erstellen des Formulars abgefragt.

## Konventionen

- Alles, was Teilnehmende sehen, in **Sie-Form**, Schweizer Schreibweise (kein ß).
- Nie „alte“, sondern „erfahrene Stellensuchende“.
- CSP-Farben: Orange `#F79C31`, Dunkel `#1A1A1A`.
- Beispielbranchen: Logistik, Detailhandel, Bau, Industrie (Seeland).
- Alle Farben als CSS-Tokens, helles und dunkles Theme.

## Offene Punkte (mit Hugo klären)

1. **Netzwerk-Tab:** Hugos A5-Tool „Mein Netzwerk“ (separate HTML-Datei, liegt bei Hugo) übernehmen, inklusive JSON-Import. Aus einer Zielfirma soll per Tipp eine Initiativbewerbung im Tracker entstehen.
2. **Kompetenzen-Tab:** Import aus dem Kompetenz-Spiegel (JSON), Belege pro Kompetenz, Top 3.
3. **Unterschrift aufs RAV-Formular** setzen (Feldposition im PDF bestimmen).
4. **Import aus „Bewerbung PDF“**: Angaben und Unterschrift aus `localStorage['bewerbung-pdf.v1']` übernehmen, wenn beide Apps auf derselben Domain laufen.
5. **Inserat-Parser:** aus eingefügtem oder gescanntem Inseratstext Firma, Stelle, Ort und Pensum vorschlagen.
6. **Android-APK** wie bei `bewerbung-pdf` (WebView mit `window.AndroidApp`-Brücke für Teilen und Speichern).
7. **Zwischenverdienst-Stunden** pro Tag erfassen, zum Abgleich mit der Bescheinigung 716.105.

## Testen

```bash
python3 -m http.server 8000   # dann http://localhost:8000
```

Getestet im Chat: Tracker, Brief-Assistent bis PDF, RAV-Formular mit 16 Einträgen (4 Seiten), ALK und Krankheit, Dossier-Stapel. Nicht getestet: echtes iPhone und Android-Gerät, Teilen-Dialog, Service Worker auf GitHub Pages.
