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
| Kompetenzen & Ressourcen | A1.II, A1.III | fertig: vier Bereiche mit Beleg pro Kompetenz, Top 3 mit Stern, Marktbezug, Ressourcen-Koffer mit Ampel |
| Strategie & Aktionsplan | A1.IV, A1.V | fertig: Bewerbungs-Tracker, Aktionsplan, **Zielrichtungen** (Hugos Werkzeug BIN 6: Passung, Markt mit Job-Room-Zahlen, Verankerung, Können, Aufwand, Matrix, Kurz-SWOT, Nachfassen), Rahmen |
| Dossier & Portfolio | A2 | fertig: Dokumentstapel, Upload, Brief-Assistent, beco-Check |
| Netzwerk | A3–A5 | fertig: aus Hugos A5-Tool portiert (Kontakte, Karte nach Nähe oder Unterstützung, Aktivieren), Import `mein-netzwerk.json`, **Zielfirmen = Hugos Werkzeug BIN 1** (Firmenrecherche mit Signal, Ansprechperson, Aufhänger, Pipeline; Initiativbewerbung mit Satzrahmen, Selbstcheck, KI-Aufträgen) |
| RAV & ALK | – | fertig: Arbeitsbemühungen, Formular 716.007 mit Unterschrift, ALK-Monatsordner, Zwischenverdienst-Stunden, Krankheit |

## Bedienung (Stand 6. November 2026)

- **Heute** ist die Startseite: Warnungen (RAV-Frist, Monatsformular, fehlendes Arztzeugnis, überfällige Schritte), die nächsten fünf Schritte aus Aktionsplan, Bewerbungen, Firmen und Netzwerk, RAV-Stand des Monats und **Mein Fokus** nach Kurswoche (W1 bis W4, Selbständig) mit Abkürzungen.
- **Plus-Knopf** unten rechts für alles Neue (Bewerbung, Dokument, Schritt, Kontakt, Firma, Zielrichtung, Krankmeldung).
- **Höchstens zwei Ebenen:** Die Initiativbewerbung aus BIN 1 liegt im Dossier (Bereich „Initiativbewerbung“), die Firmenrecherche im Netzwerk. Technisch wird der Bereich `#fr-tab-brief` beim Start ins Dossier verschoben; das CSS von BIN 1 hängt deshalb an der Klasse `.frs`, nicht an der ID.
- **Schritt für Schritt:** Die Formulare von BIN 6 (Zielrichtung) und BIN 1 (Betrieb, Initiativbrief) zeigen eine Frage nach der anderen mit Fortschrittsbalken (`wizard()`).
- **Punkte auf den Tab-Symbolen** zeigen, wo etwas fehlt oder fällig ist.
- **Einführung** beim ersten Öffnen (Name, Beispiele oder leer, Home-Bildschirm auf dem iPhone, grosse Schrift); später über die Einstellungen auf der Startseite.
- **Grosse Schrift** als Einstellung (`zoom` auf dem Inhalt).
- Die Sprache ist bewusst noch nicht vereinfacht (Hugos Entscheid). Kandidaten: „Tracker“, „Pipeline“, „Signal“, „AvP“.

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
- `netz{contacts[{id,name,sec,near,sup[],knows[],step,act}],nextId}` (gleiches Format wie `mein-netzwerk.json` des A5-Tools), `firmen[]`
- `zr[]`: Zielrichtungen im Format `optionen` aus `Meine_Zielrichtungen.json` (BIN 6), `fr{firmen[]}`: Betriebe im Format `firmen` aus `Meine_Firmenrecherche.json` (BIN 1). Beide Dateien lassen sich im jeweiligen Werkzeug einlesen und wieder sichern.
- `komp{items[{id,bereich,name,beleg,inserat,star}],res{}}`
- `check{}` (beco-Checkliste), `meta{lastBackup, docsSeeded}`
- Beispieldaten tragen `beispiel:true` und lassen sich im Tracker mit einem Knopf entfernen.

## Eingebaute Kurswerkzeuge (BIN 1 und BIN 6)

Hugos Werkzeuge „Firmenrecherche und Initiativbewerbung“ (BIN 1) und „Meine Zielrichtungen vergleichen“ (BIN 6) sind praktisch unverändert übernommen: Logik, Texte, Einstufungsregeln, Satzrahmen, Selbstcheck und KI-Aufträge stammen 1:1 aus den Originalen. Angepasst wurde nur:

- IDs mit Präfix `zr-` bzw. `fr-`, CSS unter `#zr-root` / `#fr-root` eingegrenzt, kollidierende Klassen umbenannt (`x-card`, `x-btn-ghost`, `x-tab` …), Farben auf die App-Tokens gelegt (helles und dunkles Theme).
- Speichern in `S.zr` / `S.fr` statt nur als Datei; „Drucken“ und „Sitzung beenden“ entfernt; `confirm()` ersetzt (zweimal tippen zum Löschen).
- Verbindungen: Firmen sehen die verfolgten Zielrichtungen (nicht „Zurückstellen“, nicht „Passt nicht“); Stufe „Kontaktiert“ oder weiter legt einen Eintrag im Bewerbungs-Tracker an und zählt damit fürs RAV-Formular; „Als PDF mit Briefkopf und Unterschrift“ übergibt den Initiativbrief an den Brief-Assistenten; Frage 2 der Zielrichtungen zeigt die Anzahl Betriebe aus der Firmenliste.

Wenn Hugo die Originalwerkzeuge weiterentwickelt, müssen die Änderungen hier nachgezogen werden. Die Funktionen liegen in `index.html` zwischen den Kommentaren „Zielrichtungen (Werkzeug BIN 6)“ und „Firmenrecherche und Initiativbewerbung (Werkzeug BIN 1)“.

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

## Offene Punkte

1. **Android-APK** wie bei `bewerbung-pdf` bauen (WebView). Die App nutzt für Senden `navigator.share` mit Fallback auf Download; in der WebView braucht es dafür die gleiche `window.AndroidApp`-Brücke wie in `bewerbung-pdf` (`sharePdf`, `savePdf`), oder einen DownloadListener.
2. **Auf echten Geräten testen:** iPhone (Home-Bildschirm, Teilen, Kamera im Dateiwähler), Android, Moodle.
3. **Import aus dem Kompetenz-Spiegel** (JSON), sobald Hugo die Datei liefert. Der Tab läuft auch ohne.
4. Die 3D-Ansicht des A5-Tools wurde bewusst weggelassen (Bibliothek 1.2 MB). Die 2D-Karte zeigt dieselben Verbindungen.

Erledigt in dieser Runde: Unterschrift auf dem RAV-Formular, Import der Angaben aus „Bewerbung PDF“ (gleiche Domain, `localStorage['bewerbung-pdf.v1']`), Inserat-Parser (Text-Inserat im Dossier → „Bewerbung daraus erfassen“), Zwischenverdienst-Stunden im ALK-Monat.

## Testen

```bash
python3 -m http.server 8000   # dann http://localhost:8000
```

Getestet im Chat (Chrome, lokal und offline, hell und dunkel): Zielrichtungen mit Matrix, Firmenrecherche mit Übergabe an Tracker und Brief-Assistent, Tracker, Brief-Assistent mit Unterschrift bis PDF, RAV-Formular mit 16 Einträgen (4 Seiten) und Unterschrift, ALK, Zwischenverdienst, Krankheit, Dossier-Stapel, Netzwerk mit Karte und Zielfirmen, Kompetenzen. Nicht getestet: echtes iPhone und Android-Gerät, Teilen-Dialog, Service Worker auf GitHub Pages.
