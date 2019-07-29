package treetests

import javatree.JavaTree
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import tree.Tree
import java.io.StringReader
import javax.json.Json
import javax.json.JsonObject

abstract class TreeTests {
    // method under test
    private fun parse(json: String) = doParse(json.toJsonObject())

    abstract fun doParse(jsonObject: JsonObject): Tree

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

        val tree = parse(json)

        tree.root shouldBe "class"
        tree.getChildren("class").shouldContainOnly("method", "field")
        tree.getChildren("method").shouldBeEmpty()
        tree.getChildren("field").shouldBeEmpty()
        tree.loops.shouldBeEmpty()
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

        val tree = parse(json)

        tree.root shouldBe "package"
        tree.getChildren("package").shouldContainOnly("class")
        tree.getChildren("class").shouldBeEmpty()
        tree.loops.shouldContainOnly("package")
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

class JavaTests : TreeTests() {
    override fun doParse(jsonObject: JsonObject): Tree = JavaTree.parse(jsonObject)
}

class KotlinTests : TreeTests() {
    override fun doParse(jsonObject: JsonObject) = kotlintree.parse(jsonObject)
}

private fun <E> Collection<E>.shouldContainOnly(vararg expected: E) {
    Assertions.assertThat(this).containsOnly(*expected)
}

private fun <E> Collection<E>.shouldBeEmpty() {
    Assertions.assertThat(this).isEmpty()
}

private fun String.toJsonObject(): JsonObject = Json.createReader(StringReader(this)).readObject()

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
 