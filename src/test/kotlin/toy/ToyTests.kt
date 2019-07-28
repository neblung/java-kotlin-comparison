package toy

import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.io.StringReader
import javax.json.Json
import javax.json.JsonObject

class ToyTests {
    // method under test
//    private fun parse(json: String) = JToy.parse(json.toJsonObject())
    private fun parse(json: String) = parseToy(json.toJsonObject())

    @Test
    fun `get started`() {
        val json = """
          {
            "root": {
              "name": "class",
              "children": [
                {
                  "name": "method"
                },
                {
                  "name": "field"
                }
              ]
            }
          }
        """

        val toy = parse(json)

        toy.root shouldBe "class"
        toy.getChildren("class").shouldContainOnly("method", "field")
        toy.getChildren("method").shouldBeEmpty()
        toy.getChildren("field").shouldBeEmpty()
        toy.loops.shouldBeEmpty()
    }

    @Test
    fun `loop node`() {
        val json = """
          {
            "root": {
              "name": "package",
              "children": [
                {
                  "name": "class"
                },
                {
                  "loop": true
                }
              ]
            }
          }
        """

        val toy = parse(json)

        toy.root shouldBe "package"
        toy.getChildren("package").shouldContainOnly("class")
        toy.getChildren("class").shouldBeEmpty()
        toy.loops.shouldContainOnly("package")
    }

    @Test
    fun `loop in root -- should be rejected`() {
        val json = """
          {
            "root": {
              "loop": true
            }
          }
        """

        val thrown = assertThrows<IllegalStateException> {
            parse(json)
        }

        thrown.message shouldBe "[] LOOP IN ROOT"
    }

    @Test
    fun `bad children definition -- should be reported with path`() {
        val json = """
          {
            "root": {
              "name": "myroot",
              "children": [
                {
                  "name": "subnode",
                  "children": {
                    "badly": "configured",
                    "mustBe": "array"
                  }
                }
              ]
            }
          }
        """

        val thrown = assertThrows<IllegalStateException> {
            parse(json)
        }

        thrown.message shouldBe "[myroot.subnode] children must be array: OBJECT"
    }
}

private fun <E> Collection<E>.shouldContainOnly(vararg expected: E) {
    Assertions.assertThat(this).containsOnly(*expected)
}

private fun <E> Collection<E>.shouldBeEmpty() {
    Assertions.assertThat(this).isEmpty()
}

private fun String.toJsonObject(): JsonObject = Json.createReader(StringReader(this)).readObject()

private infix fun JsonObject?.shouldBe(expected: String?) {
    when (expected) {
        null -> Assertions.assertThat(this).isNull()
        else -> Assertions.assertThat(this).isEqualTo(expected.toJsonObject())
    }
}

private infix fun String?.shouldBe(expected: String?) {
    when (expected) {
        null -> Assertions.assertThat(this).isNull()
        else -> Assertions.assertThat(this).isEqualTo(expected)
    }
}
// Java-Varianten der Getter sind eigentlich noch gegen Veränderung zu schützen.
// Bei API müssten wir das machen. Bei internals macht das natürlich keiner
//
// Zeremonie: member der Klasse (root, childMap, loops) tauchen in Java 4 Mal auf, in Kotlin 1 Mal
// bei root, loops kommen noch 2 Nennungen wegen der Getter dazu
//
// XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX!! Pfad zum Fehlerfall
// addLoopNode: 6 Klammerpaare vs 2. Java Durchsicht verstellt.
// Java-Variante obwohl klein, dennoch schwer zu verstehen
// Konsequenz: Meide Optional<T>
//
// recurse():
//   - streaming-API sperrig.  =>  for-Schleifen oft besser lesbar. Schade eigentlich
//   - cast  as
// children():
//   - cast trotz instanceof
//   - String-Interpolation
 