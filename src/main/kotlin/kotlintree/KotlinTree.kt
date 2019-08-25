package kotlintree

import tree.Tree
import java.util.*
import javax.json.JsonArray
import javax.json.JsonObject
import javax.json.JsonValue

class KotlinTree(
    private val root: String,
    private val childMap: Map<String, List<String>>,
    private val loops: Collection<String>
) : Tree {
    override fun getRoot() = root
    override fun getLoops() = loops
    override fun getChildren(parent: String) = childMap[parent] ?: emptyList()
}

fun parse(jsonTree: JsonObject) = Builder().build(jsonTree.getJsonObject("root"))

private class Builder {
    private val childNames = mutableMapOf<String, List<String>>()
    private val loops = mutableSetOf<String>()
    private val nameStack = LinkedList<String>()

    fun build(root: JsonObject): KotlinTree {
        val rootName = walk(root) ?: error("won't return null for root")
        return KotlinTree(rootName, childNames, loops)
    }

    private fun walk(parent: JsonObject): String? {
        if (isLoop(parent)) {
            addLoopNode()
            return null // do not recurse. Loop nodes are always leaf nodes
        }
        val parentName = nameOf(parent)
        nameStack.push(parentName)
        childNames[parentName] = children(parent)?.let { namesOf(it) } ?: emptyList()
        nameStack.pop()
        return parentName
    }

    private fun isLoop(node: JsonObject) = node.getBoolean("loop", false)

    private fun nameOf(node: JsonObject): String {
        return node.getString("name", null) ?: badConfiguration("node without name")
    }

    private fun addLoopNode() {
        loops += nameStack.peek() ?: badConfiguration("LOOP IN ROOT")
    }

    private fun children(parent: JsonObject): JsonArray? {
        return when (val value = parent["children"]) {
            null, JsonValue.NULL -> null
            is JsonArray -> value
            else -> badConfiguration("children must be array: ${value.valueType}")
        }
    }

    private fun namesOf(children: JsonArray) = children.mapNotNull { walk(it as JsonObject) }

    private fun badConfiguration(message: String): Nothing {
        val path = nameStack.asReversed().joinToString(".")
        throw IllegalStateException("[$path] $message")
    }
}