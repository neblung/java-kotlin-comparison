package kotlintree

import tree.Tree
import java.util.*
import javax.json.JsonArray
import javax.json.JsonObject
import javax.json.JsonValue

class KotlinTree(
    private val _root: String,
    private val childMap: Map<String, List<String>>,
    private val _loops: Collection<String>
) : Tree {
    override fun getRoot() = _root
    override fun getLoops() = _loops
    override fun getChildren(parent: String) = childMap[parent] ?: emptyList()
}

fun parse(jsonTree: JsonObject) = Builder().build(jsonTree.getJsonObject("root"))

private class Builder {
    private val childMap = mutableMapOf<String, List<String>>()
    private val loops = mutableSetOf<String>()
    private val nameStack = LinkedList<String>()

    fun build(root: JsonObject): KotlinTree {
        val rootName = walk(root) ?: error("returns never null for root")
        return KotlinTree(rootName, childMap, loops)
    }

    private fun walk(node: JsonObject): String? {
        if (node.getBoolean("loop", false)) {
            addLoopNode()
            return null // do not recurse. Loop nodes are always leaf nodes
        }
        val name = node.getString("name", null) ?: badConfiguration("node without name")
        nameStack.push(name)
        children(node)?.let { recurse(it) }
        nameStack.pop()
        return name
    }

    private fun addLoopNode() {
        loops += nameStack.peek() ?: badConfiguration("LOOP IN ROOT")
    }

    private fun recurse(children: JsonArray) {
        val parentName = nameStack.peek()
        val childNames = children.mapNotNull { walk(it as JsonObject) }
        childMap[parentName] = childNames
    }

    private fun children(parent: JsonObject): JsonArray? {
        return when (val value = parent["children"]) {
            null, JsonValue.NULL -> null
            is JsonArray -> value
            else -> badConfiguration("children must be array: ${value.valueType}")
        }
    }

    private fun badConfiguration(message: String): Nothing {
        val path = nameStack.asReversed().joinToString(".")
        throw IllegalStateException("[$path] $message")
    }
}