import java.io.IOException;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
/** A small persistent key-value store (tab-separated file);
not a full SQL database. */
public class MiniDatabase {
private final Path file;
private final Map<String, String> rows = new
LinkedHashMap<>();
public MiniDatabase(Path file) throws IOException {
this.file = file;
load();
}
// ---------- persistence ----------
private void load() throws IOException {
if (!Files.exists(file)) return;
List<String> lines;
try {
lines = Files.readAllLines(file,
StandardCharsets.UTF_8);
} catch (CharacterCodingException e) {
throw new IOException("Database file is not valid
UTF-8: " + file, e);
}
for (String line : lines) {
if (line.isEmpty()) continue;
String[] parts = line.split("\t", 2); // real tab
(the old code wrote a literal "\t")
if (parts.length == 2) rows.put(decode(parts[0]),
decode(parts[1]));
}
}
/** Writes to a temp file, then atomically replaces the
real one so a crash can't lose data. */
private void save() throws IOException {
List<String> lines = new ArrayList<>();
rows.forEach((k, v) -> lines.add(encode(k) + "\t" +
encode(v)));
Path abs = file.toAbsolutePath();
Path tmp = abs.resolveSibling(abs.getFileName() +
".tmp");
Files.write(tmp, lines, StandardCharsets.UTF_8,
StandardOpenOption.CREATE,
StandardOpenOption.TRUNCATE_EXISTING);
try {
Files.move(tmp, abs,
StandardCopyOption.REPLACE_EXISTING,
StandardCopyOption.ATOMIC_MOVE);
} catch (AtomicMoveNotSupportedException e) {
Files.move(tmp, abs,
StandardCopyOption.REPLACE_EXISTING);
}
}
private static String encode(String s) {
return s.replace("\\", "\\\\").replace("\t", "\
\t").replace("\n", "\\n").replace("\r", "\\r");
}
private static String decode(String s) {
StringBuilder out = new StringBuilder();
boolean esc = false;
for (int i = 0; i < s.length(); i++) {
char c = s.charAt(i);
if (esc) {
out.append(c == 't' ? '\t' : c == 'n' ? '\n' :
c == 'r' ? '\r' : c);
esc = false;
} else if (c == '\\') {
esc = true;
} else {
out.append(c);
}
}
if (esc) out.append('\\');
return out.toString();
}
// ---------- operations ----------
/** @return false if the key already exists */
public boolean insert(String key, String value) throws
IOException {
if (rows.containsKey(key)) return false;
rows.put(key, value);
save();
return true;
}
/** @return false if the key does not exist */
public boolean update(String key, String value) throws
IOException {
if (!rows.containsKey(key)) return false;
rows.put(key, value);
save();
return true;
}
public Optional<String> get(String key) { return
Optional.ofNullable(rows.get(key)); }
public boolean delete(String key) throws IOException {
if (rows.remove(key) == null) return false;
save();
return true;
}
public int count() { return rows.size(); }
public Map<String, String> all() { return
Collections.unmodifiableMap(rows); }
// ---------- CLI ----------
private void run() {
Scanner sc = new Scanner(System.in,
StandardCharsets.UTF_8);
System.out.println("MiniDatabase - type HELP for
commands");
while (true) {
System.out.print("db> ");
if (!sc.hasNextLine()) return;
String line = sc.nextLine().trim();
if (line.isEmpty()) continue;
String[] p = line.split("\\s+", 3);
String cmd = p[0].toUpperCase();
try {
switch (cmd) {
case "EXIT", "QUIT" -> { return; }
case "HELP" -> System.out.println(
"INSERT id value | UPDATE id value
| GET id | DELETE id | LIST | COUNT | EXIT");
case "LIST" -> rows.forEach((k, v) ->
System.out.println(k + " = " + v));
case "COUNT" ->
System.out.println(count());
case "INSERT" -> {
if (p.length < 3)
{ System.out.println("Usage: INSERT id value"); break; }
System.out.println(insert(p[1], p[2]) ?
"OK" : "Error: key exists (use UPDATE)");
}
case "UPDATE" -> {
if (p.length < 3)
{ System.out.println("Usage: UPDATE id value"); break; }
System.out.println(update(p[1], p[2]) ?
"OK" : "Error: no such key");
}
case "GET" -> {
if (p.length < 2)
{ System.out.println("Usage: GET id"); break; }
System.out.println(get(p[1]).orElse("(nil)"));
}
case "DELETE" -> {
if (p.length < 2)
{ System.out.println("Usage: DELETE id"); break; }
System.out.println(delete(p[1]) ? "1" :
"0");
}
default -> System.out.println("Unknown
command (try HELP)");
}
} catch (IOException e) {
System.out.println("I/O error: " +
e.getMessage());
}
}
}
public static void main(String[] args) {
Path path = Path.of(args.length > 0 ? args[0] :
"minidb.tsv");
try {
new MiniDatabase(path).run();
} catch (IOException e) {
System.err.println("Could not open database: " +
e.getMessage());
System.exit(1);
}
}
}