# Database_Marth
MiniDatabase

Ein kleiner persistenter Key-Value-Speicher in Java mit interaktiver Kommandozeile. Die Daten werden in einer Textdatei (tabulatorgetrennt) gespeichert und bleiben nach einem Neustart erhalten.

Das Projekt besteht aus einer einzigen Datei ohne Abhängigkeiten. Es ist keine SQL-Datenbank (siehe Einschränkungen).

Voraussetzungen
Java 17 oder neuer
Starten
bash
# direkt aus dem Quelltext (Java 11 oder neuer erforderlich, empfohlen: 17+)
java MiniDatabase.java

# oder zuerst kompilieren
javac MiniDatabase.java
java MiniDatabase

Standardmäßig werden die Daten in minidb.tsv im aktuellen Verzeichnis gespeichert. Eine andere Datei gibst du als erstes Argument an:

bash
java MiniDatabase.java meine-daten.tsv
Befehle
Befehl	Beschreibung
INSERT id wert	Neuen Eintrag anlegen. Schlägt fehl, wenn id schon existiert.
UPDATE id wert	Vorhandenen Eintrag überschreiben. Schlägt fehl, wenn id nicht existiert.
GET id	Wert ausgeben, oder (nil), falls es die id nicht gibt.
DELETE id	Gibt 1 aus, wenn gelöscht wurde, sonst 0.
LIST	Alle Einträge ausgeben.
COUNT	Anzahl der Einträge ausgeben.
HELP	Befehlsübersicht anzeigen.
EXIT / QUIT	Programm beenden.

Befehle sind nicht von der Groß- und Kleinschreibung abhängig. Eine id darf keine Leerzeichen enthalten, ein Wert darf welche enthalten. Über die Kommandozeile werden Leerzeichen am Anfang und Ende der ganzen Eingabezeile entfernt.

Beispiel
db> INSERT user:1 Ada Lovelace
OK
db> GET user:1
Ada Lovelace
db> INSERT user:1 jemand anderes
Error: key exists (use UPDATE)
db> UPDATE user:1 Ada King
OK
db> COUNT
1
db> EXIT

Starte das Programm danach erneut: GET user:1 liefert weiterhin Ada King.

Die Meldungen des Programms sind auf Englisch.

Dateiformat

Pro Eintrag eine Zeile, schlüssel<TAB>wert, in UTF-8:

user:1	Ada King
note	erste Zeile\nzweite Zeile

Damit jeder Wert unverändert gespeichert werden kann, werden vier Zeichen maskiert: Backslash (\\), Tabulator (\t), Zeilenumbruch (\n) und Wagenrücklauf (\r). Ein Wert mit Zeilenumbruch bleibt dadurch trotzdem in einer Zeile der Datei. Dasselbe gilt für Schlüssel.

Verwendung als Bibliothek

Die Klasse lässt sich auch ohne Kommandozeile im eigenen Code verwenden:

java
MiniDatabase db = new MiniDatabase(Path.of("daten.tsv"));

db.insert("user:1", "Ada Lovelace");     // true; false, wenn der Schlüssel schon existiert
db.update("user:1", "Ada King");         // true; false, wenn der Schlüssel fehlt
Optional<String> name = db.get("user:1");
db.delete("user:1");                     // true; false, wenn der Schlüssel fehlt
int anzahl = db.count();
Map<String, String> alle = db.all();     // schreibgeschützte Ansicht

insert, update und delete speichern sofort in die Datei und können eine IOException werfen.

So funktioniert es
Absturzsicheres Speichern. Jede Änderung wird zuerst in eine temporäre Datei geschrieben und dann atomar über die echte Datei verschoben. Stürzt das Programm mitten im Schreiben ab, bleibt die vorherige Version intakt. Unterstützt das Dateisystem kein atomares Verschieben, wird auf normales Ersetzen zurückgegriffen.
Beliebige Werte. Dank der Maskierung (siehe oben) überstehen auch Tabulatoren, Zeilenumbrüche, Backslashes und Unicode (zum Beispiel ü, ✓, 日本語) das Speichern und Laden unverändert.
Reihenfolge bleibt erhalten. Intern wird eine LinkedHashMap verwendet, LIST zeigt die Einträge in der Reihenfolge des Einfügens.
Robustes Laden. Leere Zeilen und Zeilen ohne Tabulator werden übersprungen. Eine Datei, die kein gültiges UTF-8 ist, führt zu einer verständlichen Fehlermeldung statt zu einem Absturz mit Stacktrace.
Datei wird erst beim Schreiben angelegt. Wer die Datenbank nur öffnet, erzeugt noch keine Datei.
Tests

Die Datei tests/MiniDatabaseTest.java enthält 22 Tests für JUnit 5. Sie prüfen unter anderem:

Einfügen, Lesen, Ändern und Löschen, auch bei fehlenden und doppelten Schlüsseln
Persistenz über einen Neustart hinweg (neue Instanz, gleiche Datei)
Rundlauf kniffliger Werte und Schlüssel (Tabulator, Zeilenumbruch, Backslash, Unicode, leerer Wert)
das Dateiformat, darunter ein echter Tabulator als Trennzeichen
beschädigte Dateien: leere Zeilen, Zeilen ohne Tabulator, ungültiges UTF-8
dass nach dem Speichern keine temporäre Datei zurückbleibt

Mit dem JUnit-Konsolen-Programm lassen sie sich ohne Maven oder Gradle ausführen:

bash
curl -L -o junit-console.jar https://repo1.maven.org/maven2/org/junit/platform/junit-platform-console-standalone/1.10.2/junit-platform-console-standalone-1.10.2.jar
mkdir -p out
javac -cp junit-console.jar -d out MiniDatabase.java tests/MiniDatabaseTest.java
java -jar junit-console.jar --class-path out --scan-class-path

Alternativ kannst du die Tests in jeder Java-IDE (IntelliJ IDEA, Eclipse, VS Code) direkt ausführen.

Einschränkungen
Keine SQL-Datenbank. Es gibt nur Zugriff über den Schlüssel: keine Tabellen, Spalten, Abfragen, Indizes oder Transaktionen.
Ganze Datei bei jeder Änderung. Bei jeder Änderung wird die komplette Datei neu geschrieben. Für kleine Datenmengen ist das in Ordnung, bei großen wird es langsam.
Nicht threadsicher. Die Klasse ist für einen Benutzer in einem Programm gedacht.
Keine Dateisperre. Greifen zwei Programme gleichzeitig auf dieselbe Datei zu, überschreiben sie sich gegenseitig.
Nur Texte. Schlüssel und Werte sind Zeichenketten.
Ideen für Erweiterungen
Statt die ganze Datei neu zu schreiben, nur Änderungen anhängen (Append-only-Log) und die Datei gelegentlich zusammenfassen.
Dateisperre hinzufügen, damit mehrere Programme sicher dieselbe Datei verwenden können.
Mit MiniRedisServer kombinieren, sodass der Netzwerk-Server seine Daten auf der Festplatte speichert.
Ablaufzeiten für Einträge (TTL) einführen.