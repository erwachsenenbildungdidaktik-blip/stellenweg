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

- **Heute** ist die Startseite: Warnungen (RAV-Frist, Monatsformular, fehlendes Arztzeugnis, überfällige Schritte) fest oben, darunter **Widgets** in einem Raster mit 4 Spalten (`WIDGETS`, Anordnung in `S.meta.home` als `{id,k,w,h,cfg}`). Katalog: Nächste Schritte, RAV-Stand, Bewerbungen, Mein Fokus, Personalberatung, Zielfirmen, Argumente, Netzwerk, Dossier, Kasse, Krankheit, Schnellzugriff (mehrfach, Ziel in `cfg`). Jedes Widget hat erlaubte Grössen; der Inhalt passt sich an.
- **Bearbeiten** über „Startseite anpassen“ oder langes Drücken: Widgets wackeln, Ziehen verschiebt (andere weichen federnd aus), die Ecke unten rechts skaliert diagonal und rastet auf die nächste erlaubte Grösse ein (Tippen auf die Ecke wechselt die Grösse), Minus entfernt, „+ Widget“ fügt hinzu, „Zurücksetzen“ (zweimal tippen) stellt den Standard her. Tastatur: Pfeile verschieben, +/− Grösse, Entf. **Untere Leiste:** im selben Modus Symbole nach vorne oder hinten ziehen (`S.meta.tabs`). Beendet mit „Fertig“, Escape oder Tipp auf eine freie Stelle.
- **Hinweise als Popup:** Warnungen erscheinen beim Öffnen der App als Dialog (einmal pro Sitzung, nicht während Einführung oder Rundgang), danach über die Glocke oben rechts mit Zähler (rot bei dringenden). Ein Tipp auf einen Hinweis führt direkt hin.
- **Brief-Assistent:** Briefkopf als Wahl „Klassisch“ oder „Farbig“. Keine Beilagen im Brief (Schweizer Praxis). Schritt 5: „Per E-Mail senden“ speichert das PDF und öffnet das Standard-Mailprogramm (`mailto:`) mit Empfänger, Betreff und Begleittext; den Anhang fügt man selbst hinzu, weil `mailto:` keine Anhänge kann. „Teilen“ schickt das PDF direkt als Anhang.
- **Beispieldokumente:** Lebenslauf (zweispaltig: links Kontakt, Geburtsdatum, Nationalität mit Bewilligung, Kinder mit Jahrgang und Betreuung, Führerausweis/Fahrzeug, Verfügbarkeit, Sprachen nach Europäischem Sprachenportfolio, IKT, Engagement; rechts Kurzprofil mit Slogan, Berufserfahrung, Aus- und Weiterbildung), Bewerbungsschreiben (linksbündig, Ort und Datum über dem Betreff, Abschnitte Sie, Ich, Wir) und Arbeitszeugnis einer erfundenen Person (Marco Beispiel), als Canvas gezeichnet. `EXV` erhöhen, wenn sich ihr Inhalt ändert; alte Beispiele werden dann beim Start ersetzt.
- **Kurs oder selbständig** (`S.meta.woche`: 0 = selbständig, 1–4 = Kurswoche; Frage in Einrichtung und Einstellungen, `applyMode()`): Im Kurs zeigt die untere Leiste „Kurs“; darin eine eigene Leiste mit Kompetenzen und Strategie (`#kursbar`, zuletzt benutzte Seite in `S.meta.kursTab`). Selbständig: Kompetenzen fällt weg, Strategie heisst „Bewerbungen“ (ohne Zielrichtungen), im Netzwerk fallen Karte und Aktivieren weg, Kurskürzel (`.eyebrow.kurs`) verschwinden, „Mein Fokus“ wird „Meine Stellensuche“, Rundgang (`tourChapters()`), Plus-Knopf und Widget-Katalog passen sich an.
- **Plus-Knopf** unten rechts für alles Neue (Bewerbung, Dokument, Schritt, Kontakt, Firma, Zielrichtung, Krankmeldung).
- **Höchstens zwei Ebenen:** Die Initiativbewerbung aus BIN 1 liegt im Dossier (Bereich „Initiativbewerbung“), die Firmenrecherche im Netzwerk. Technisch wird der Bereich `#fr-tab-brief` beim Start ins Dossier verschoben; das CSS von BIN 1 hängt deshalb an der Klasse `.frs`, nicht an der ID.
- **Schritt für Schritt:** Die Formulare von BIN 6 (Zielrichtung) und BIN 1 (Betrieb, Initiativbrief) zeigen eine Frage nach der anderen mit Fortschrittsbalken (`wizard()`).
- **Punkte auf den Tab-Symbolen** zeigen, wo etwas fehlt oder fällig ist.
- **Einrichtung** beim ersten Öffnen (`openIntro()`, Schritte in `ONB`): ganzer Bildschirm, vor dem Rundgang. Willkommen mit Farbwahl, Über Sie (Name ist Pflicht), Briefkopf, Unterschrift, Unterlagen (Lebenslauf, Zeugnisse, Diplome, Foto, mit Kamera oder Datei), RAV und Kasse (Personalberatung, Termin, Vorgabe, Kasse, Rahmenfrist), Kurswoche, Mitteilungen, grosse Schrift. Zu jedem Feld ein „?“-Tipp; der erste Tipp eines Schritts öffnet sich von selbst. Am Ende eine Übersicht mit „Ergänzen ›“ für Fehlendes, dann Rundgang. Wer einrichtet, startet ohne Beispiele; „Erst mit Beispielen umschauen“ behält sie. Wieder aufrufbar in den Einstellungen.
- **App-Farbschema** (`APP_THEMES`, `S.meta.color`): Orange, Südhang (Jurakalk und Reben; intern `jura`, frühere Wahl „Seeblau“ wird dazu), Wind (Sturm-Türkis, „Frisch und kräftig wie der Joran“; intern `joran`, frühere Wahl „Beere“ wird dazu) oder eigene Farbe; je ein Satz Farben für hell und dunkel, per `<style id="app-theme">`.
- **App weitergeben** (Einstellungen → Hilfe): iPhone oder Android wählen, dann QR-Code (offline erzeugt mit `vendor/qrcode.min.js`) zur Web-App bzw. zur APK, Installationsschritte, Link kopieren oder teilen. Adresse in `APP_URL`.
- **Seitenleiste** am rechten Rand: schwebender Griff (etwa 2 cm × 3 mm), nach links ziehen oder antippen öffnet die Schnellauswahl, senkrecht ziehen verschiebt den Griff. Standard: Job-Room, E-Mail, ChatGPT; das Plus fügt aus dem Katalog `SIDE_APPS` oder eigene Verknüpfungen (`S.meta.sideCustom`) hinzu. Lange drücken hebt einen Eintrag an: hoch oder runter ziehen verschiebt, zur Seite wischen entfernt. Reihenfolge in `S.meta.side`. Eine Web-App kann installierte Apps nicht auflisten oder starten; es sind Webadressen, `tel:`, `mailto:` und `wa.me`, die das Handy in der passenden App öffnet.
- **Auto-Update beim Start** (`checkUpdate()`): Web/iPhone über den Service Worker plus Hinweis bei neuer Version; Android vergleicht mit `version.json` (vom Build geschrieben) und installiert die neue APK über die Brücke (`installUpdate`, nur von github.io). Details in `../README.md`.
- **Mitteilungen** (`S.meta.notify`, ein- und ausschaltbar): Die App schreibt kommende Erinnerungen nach kv `notify` (Abgabefenster am 22., Frist am 3., AvP am 1., RAV-Termin am Vorabend, geplante Schritte, Firmen-Schritte, Arztzeugnis ab Tag 4, aktuelle Warnungen). `sw.js` zeigt fällige an (periodische Hintergrund-Synchronisation, Nachricht `check` von der Seite) und merkt sie in kv `notified`. Tipp auf die Mitteilung öffnet die richtige Stelle. Grenzen ohne Server: Android im Hintergrund etwa ein- bis zweimal täglich (installierte App), iPhone nur solange die App läuft.
- **Rundgang** direkt nach der Einführung (`startTour()`, Daten in `TOUR`): 6 Kapitel entlang der Tabs, 31 Schritte, jede Funktion mit Spotlight. Startbild mit Kapitelübersicht (direkt einsteigen möglich); in jeder Sprechblase „Kapitel überspringen“ und „Rundgang beenden“, ausserdem × und Escape. `S.meta.toured` verhindert die Wiederholung; wieder starten über die Einstellungen. Bestehende Nutzerinnen und Nutzer sehen ihn einmal beim nächsten Öffnen.
- **Einstellungen** (Zahnrad auf der Startseite), nach Bereichen: Darstellung (grosse Schrift, Farbschema automatisch/hell/dunkel in `S.meta.theme`, Animationen reduzieren in `S.meta.calm`), Kurs (Kurswoche für das Widget „Mein Fokus“), Meine Angaben (Name, Adresse, Briefkopf; dieselben Felder wie im Brief-Assistenten), Daten (Sichern und laden, Beispiele entfernen, alle Daten löschen in zwei Schritten), Hilfe (Rundgang, Einführung).
- **Grosse Schrift** wirkt per `zoom` auf den Inhalt.
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

- **Wochenziel (intern):** `wochenZiel()` = RAV-Monatsvorgabe × 1,2 ÷ 4,33, gerundet. Rund 20 % Reserve über der Vorgabe; wegen ganzer Zahlen je nach Vorgabe etwa +8 % bis +30 %. **Wird in der App bewusst nirgends erklärt** (Entscheid Kursleitung). Teilnehmende sehen nur „Ihr Wochenziel: 3 Bewerbungen“ und tragen nur die RAV-Vorgabe ein.
- **RAV-Monat:** Vom 1. bis 5. zählen Anzeige, Widget und Warnung den Vormonat (`ravMonth()`), weil dann dessen Formular fällig ist.
- **Grundunterlagen:** Lebenslauf, Arbeitszeugnisse, Diplome. Das Bewerbungsschreiben zählt nicht dazu (entsteht pro Bewerbung). Rote Punkte auf den Tabs nur bei Fristen und Fälligem.
- **Aufbewahrung:** Alles bleibt nur auf dem Gerät. Deinstallieren löscht alle Daten (APK mit `allowBackup="false"`, keine Google-Sicherung). Hinweis in den Einstellungen unter „Daten“. Keine automatische Löschung innerhalb der App.

- Arbeitsbemühungen: Abgabe ab dem 22., spätestens 5. des Folgemonats (Kanton Bern, Angabe Hugo; Formular 716.007 bestätigt den 5.).
- Krankheit (Merkblatt Krankheit, AVA Kanton Bern): sofort beim RAV melden, Arztzeugnis ab dem 4. Tag an die Kasse, Krankentage in die AvP, höchstens 30 Kalendertage am Stück, höchstens 44 Taggelder pro Rahmenfrist. Der Zähler in der App ist eine Schätzung (Werktage Mo–Fr, ohne Feiertage).
- Dossier-Check: Kurzfassung (9 Punkte, 4 davon prüft die App) der Checkliste für Bewerbungsdossiers, beco Berner Wirtschaft. In der App zugeklappt als eine Zeile mit Fortschritt; die Dokumentvorschau darüber ist dafür grösser.
- AHV-Nummer wird nie gespeichert, nur beim Erstellen des Formulars abgefragt.

## Konventionen

- Alles, was Teilnehmende sehen, in **Sie-Form**, Schweizer Schreibweise (kein ß).
- Nie „alte“, sondern „erfahrene Stellensuchende“.
- CSP-Farben: Orange `#F79C31`, Dunkel `#1A1A1A`.
- Beispielbranchen: Logistik, Detailhandel, Bau, Industrie (Seeland).
- Alle Farben als CSS-Tokens, helles und dunkles Theme.
- **Bewegung im Stil von ColorOS:** Kurven als Tokens `--spring` (Feder mit leichtem Nachschwingen), `--spring-soft` (Sheets), `--glide` (Abbremsen ohne Überschwingen), `--exit` (schnelles Wegfallen). Öffnen federt, Schliessen ist kürzer und beschleunigt. Offenes Sheet: App rückt auf dem Handy nach hinten (`html.sheet-open .app`), Hintergrund weichgezeichnet, Plus-Symbol dreht sich zum ×. Knöpfe federn beim Antippen. Bei „Bewegung reduzieren“ im System ist alles aus (Windows: Einstellungen → Barrierefreiheit → Animationseffekte).

## Offene Punkte

1. **Android-APK:** erledigt, siehe `../README.md` und `../android/`. Hülle in Java mit WebView (WebViewAssetLoader, offline), Brücke `window.AndroidApp`: installierte Apps mit Symbolen und schwebendem Fenster, Mitteilungen per AlarmManager, Teilen-Ziel ins Notizfenster, PDF teilen/speichern, Kamera, Zwischenablage, Vibration. Bauen mit `APK-bauen.cmd`. Noch nicht auf einem echten Gerät getestet.
2. **Auf echten Geräten testen:** iPhone (Home-Bildschirm, Teilen, Kamera im Dateiwähler), Android, Moodle.
3. **Import aus dem Kompetenz-Spiegel** (JSON), sobald Hugo die Datei liefert. Der Tab läuft auch ohne.
4. **Mitteilungen zuverlässiger:** In der Android-APK exakt getaktete Erinnerungen (AlarmManager); auf dem iPhone ginge das nur mit einem Push-Server.
5. Die 3D-Ansicht des A5-Tools wurde bewusst weggelassen (Bibliothek 1.2 MB). Die 2D-Karte zeigt dieselben Verbindungen.

Erledigt in dieser Runde: Unterschrift auf dem RAV-Formular, Import der Angaben aus „Bewerbung PDF“ (gleiche Domain, `localStorage['bewerbung-pdf.v1']`), Inserat-Parser (Text-Inserat im Dossier → „Bewerbung daraus erfassen“), Zwischenverdienst-Stunden im ALK-Monat.

## Testen

```bash
python3 -m http.server 8000   # dann http://localhost:8000
```

Getestet im Chat (Chrome, lokal und offline, hell und dunkel): Zielrichtungen mit Matrix, Firmenrecherche mit Übergabe an Tracker und Brief-Assistent, Tracker, Brief-Assistent mit Unterschrift bis PDF, RAV-Formular mit 16 Einträgen (4 Seiten) und Unterschrift, ALK, Zwischenverdienst, Krankheit, Dossier-Stapel, Netzwerk mit Karte und Zielfirmen, Kompetenzen. Nicht getestet: echtes iPhone und Android-Gerät, Teilen-Dialog, Service Worker auf GitHub Pages.
