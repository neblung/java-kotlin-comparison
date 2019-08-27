Eine kleine Gegenüberstellung von Java und Kotlin.

 - Ein Java-Interface [Tree](./src/main/java/tree/Tree.java) abstrahiert eine Baum-Struktur.
 - Ein [Beispiel-Programm](./src/main/java/run/Main.java) liest ein [JSON-File](./src/main/resources/sample.json)
und lässt es sowohl von einer Java- als auch von einer Kotlin-Implementierung parsen.
 - Beide Implementierungen sind durch [Tests](./src/test/kotlin/treetests/TreeTests.kt) gesichert. 

Das Beispiel-Programm und die Tests können leicht mit gradle ausgeführt werden:

    $ ./gradlew run
    $ ./gradlew test

Zweck dieses Repositorys ist die **Gegenüberstellung der [Java-Implementierung](./src/main/java/javatree/JavaTree.java) und der [Kotlin-Implementierung](./src/main/kotlin/kotlintree/KotlinTree.kt)**
und damit letztlich auch eine Werbung für Kotlin.

### Code-Umfang
Es fällt auf, dass die Java-Implementierung ca 50% mehr Quelltext benötigt als die Kotlin-Implementierung.
Das liegt nicht an unserem spezillen Anwendungsfall, sondern ist typisch. Siehe [hier](https://kotlinlang.org/docs/reference/faq.html#what-advantages-does-kotlin-give-me-over-the-java-programming-language)

### Boilerplate
Java ist geschwätzig, Kotlin ist kompakt. Fast jede Klasse initialisiert Felder durch den Konstruktor.
In Java braucht man pro Feld
 - Eine Feld-Deklaration
 - Einen Konstruktor-Parameter
 - Die Zuweisung des Parameters an das Feld


    public class JavaTree implements Tree {
        private final String root;
        private final Map<String, List<String>> childNames;
        private final Collection<String> loops;
    
        private JavaTree(String root, Map<String, List<String>> childNames, Collection<String> loops) {
            this.root = root;
            this.childNames = childNames;
            this.loops = loops;
        }

Dank [Primary Constructor](https://kotlinlang.org/docs/reference/classes.html#constructors) reicht in Kotlin
die einfache Nennung.

    class KotlinTree(
        private val root: String,
        private val childNames: Map<String, List<String>>,
        private val loops: Collection<String>
    ) : Tree {

Getter sind dank [Expression body und type inference](https://kotlinlang.org/docs/reference/basic-syntax.html#defining-functions)
deutlich kompakter.

    public String getRoot() {
        return root;
    }
    
vs
    
    fun getRoot() = root
    
Nicht falsch verstehen: Auch die Kotlin-Implementierung ist natürlich statisch getypt. Wir brauchen den
Return-Typ aber nicht notieren. Er wird vom Compiler abgeleitet.

### Nullability
Der [Umgang mit `null`](https://kotlinlang.org/docs/reference/null-safety.html) ist Kotlins große Stärke!
Siehe auch [hier](https://medium.com/@elizarov/null-is-your-friend-not-a-mistake-b63ff1751dd5).
Nicht nur, dass der Compile-Time-Schutz die meisten `NullPointerException`s vermeidet. Der Code wird auch
klarer und besser wartbar.

Beim Parsen des Json haben wir in `addLoopNode()` die Aufgabe, den Wert, der im Stack oben aufliegt,
der Liste `loops` hinzuzufügen. Sollte der Stack leer sein, ist ein Fehler mit passender Meldung auszulösen.

    private void addLoopNode() {
        loops.add(Optional.ofNullable(nameStack.peek()).orElseThrow(() -> badConfiguration("LOOP IN ROOT")));
    }

Seit 1.8 hat Java die Klasse `Optional`, die den Umgang mit `null` verbessert.
Das Konstrukt ist zwar effektiv. Allerdings ist es so geschwätzig, dass es kein no-brainer mehr ist.
In der Praxis wird den Null-Vergleichen daher oft der Vorzug gegenüber Optional gegeben.

Die Kotlin-Variante:

    private fun addLoopNode() {
        loops += nameStack.peek() ?: throw badConfiguration("LOOP IN ROOT")
    }

Zur Lesbarkeit tragen neben der Nullability maßgeblich bei:
 - [Conventions bzw Operator overloading](https://kotlinlang.org/docs/reference/operator-overloading.html)
 - [Nothing](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-nothing.html)
 
### Lambdas und Streams
Ja, seit 1.8 gibt es auch in Java Lambdas und Methodenreferenzen.
Ihre Anwendung ist allerdings alles andere als elegant.
(Die Kommentare am Zeilenende verhindern, dass die Auto-Formatierung der IDE die Lesbarkeit
wieder zerstört.)
 
    private List<String> namesOf(JsonArray children) {
        return children.stream() //
                .map(JsonObject.class::cast) //
                .map(this::walk) // recursion
                .filter(Objects::nonNull) //
                .collect(Collectors.toList());
    }

In der Praxis führt die umständliche Notation oft dazu, dass der funktionale Stil nicht gelebt wird,
sondern weiterhin for-Schleifen zum Einsatz kommen.

In Kotlin stehen Lambdas übrigens auch dann zur Verfügung, wenn die Version der Target-VM noch 1.6 ist.

    private fun namesOf(children: JsonArray) = children.mapNotNull { walk(it as JsonObject) }

### when, smart cast, string interpolation

    // Nullable
    private JsonArray children(JsonObject parent) {
        JsonValue value = parent.get("children");
        if (value == null || value.equals(JsonValue.NULL)) {
            return null;
        } else if (value instanceof JsonArray) {
            return (JsonArray) value;
        } else {
            throw badConfiguration("children must be array: " + value.getValueType());
        }
    }

vs

    private fun children(parent: JsonObject): JsonArray? {
        return when (val value = parent["children"]) {
            null, JsonValue.NULL -> null
            is JsonArray -> value
            else -> badConfiguration("children must be array: ${value.valueType}")
        }
    }

[when](https://kotlinlang.org/docs/reference/control-flow.html#when-expression),
[smart casts](https://kotlinlang.org/docs/reference/typecasts.html#smart-casts) und
[string interpolation](https://kotlinlang.org/docs/reference/basic-types.html#string-templates)
machen das Erstellen und das Lesen des Quelltextes angenehm.

Die Kotlin-Variante deklariert durch das Fragezeichen explizit, dass die Methode `null` liefern
kann (`JsonArray?`). Kümmert sich der Aufrufer nicht um den Null-Fall, so resultiert das in einem
Compile-Fehler! **Der fehlerhafte Code kann also gar nicht erst gebaut werden.**

In Java ist es so: Kümmert sich der Aufrufer nicht um den Null-Fall, so kompiliert der Code trotzdem.
Der Fehlerfall löst (nicht selten erst beim Kunden) eine `NullPointerException` aus.   

## Fazit
In Kotlin ist das Signal/Rauschen-Verhältnis deutlich besser. So gesehen ist es Verschwendung,
den Code in der Quell-Sprache Java zu notieren. Berücksichtigt man dazu die fehlende Null-Safety,
so muss man es als fahrlässig bezeichnen. 