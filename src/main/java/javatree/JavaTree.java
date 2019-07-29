package javatree;

import tree.Tree;

import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonValue;
import java.util.*;
import java.util.stream.Collectors;

public class JavaTree implements Tree {
    private final String root;
    private final Map<String, List<String>> childMap;
    private final Collection<String> loops;

    private JavaTree(String root, Map<String, List<String>> childMap, Collection<String> loops) {
        this.root = root;
        this.childMap = childMap;
        this.loops = loops;
    }

    public static JavaTree parse(JsonObject jsonTree) {
        JsonObject root = jsonTree.getJsonObject("root");
        return new Builder().build(root);
    }

    public String getRoot() {
        return root;
    }

    public List<String> getChildren(String parent) {
        return childMap.getOrDefault(parent, Collections.emptyList());
    }

    public Collection<String> getLoops() {
        return loops;
    }

    private static class Builder {
        private final Map<String, List<String>> childMap = new HashMap<>();
        private final Collection<String> loops = new HashSet<>();
        private final Deque<String> nameStack = new LinkedList<>();

        JavaTree build(JsonObject root) {
            String rootName = walk(root);
            return new JavaTree(rootName, childMap, loops);
        }

        private String walk(JsonObject node) {
            if (node.getBoolean("loop", false)) {
                addLoopNode();
                return null;// do not recurse. Loop nodes are always leaf nodes
            }
            String name = node.getString("name", null);
            if (name == null) {
                throw badConfiguration("node without name");
            }
            nameStack.push(name);
            JsonArray children = children(node);
            if (children != null) {
                recurse(children);
            }
            nameStack.pop();
            return name;
        }

        private void addLoopNode() {
            loops.add(Optional.ofNullable(nameStack.peek()).orElseThrow(
                    () -> badConfiguration("LOOP IN ROOT")));
        }

        private void recurse(JsonArray children) {
            String parentName = nameStack.peek();
            List<String> childNames = children.stream() //
                    .map(JsonObject.class::cast) //
                    .map(this::walk) //
                    .filter(Objects::nonNull) //
                    .collect(Collectors.toList());
            childMap.put(parentName, childNames);
        }

        // Nullable
        private JsonArray children(JsonObject parent) {
            JsonValue jsonValue = parent.get("children");
            if (jsonValue == null || jsonValue.equals(JsonValue.NULL)) {
                return null;
            } else if (jsonValue instanceof JsonArray) {
                return (JsonArray) jsonValue;
            } else {
                throw badConfiguration("children must be array: " + jsonValue.getValueType());
            }
        }

        private RuntimeException badConfiguration(String message) {
            Collections.reverse((List<?>) nameStack); // Seiteneffekt
            String path = String.join(".", nameStack);
            return new IllegalStateException(String.format("[%s] %s", path, message));
        }
    }
}