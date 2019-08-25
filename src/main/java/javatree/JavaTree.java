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
        return new Builder().build(jsonTree.getJsonObject("root"));
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
        private final Map<String, List<String>> childNames = new HashMap<>();
        private final Collection<String> loops = new HashSet<>();
        private final LinkedList<String> nameStack = new LinkedList<>();

        JavaTree build(JsonObject root) {
            String rootName = walk(root);
            return new JavaTree(rootName, childNames, loops);
        }

        private String walk(JsonObject parent) {
            if (isLoop(parent)) {
                addLoopNode();
                return null; // do not recurse. Loop nodes are leaf nodes
            }
            String parentName = nameOf(parent);
            nameStack.push(parentName);
            JsonArray children = children(parent);
            childNames.put(parentName, children == null ? Collections.emptyList() : namesOf(children));
            nameStack.pop();
            return parentName;
        }

        private boolean isLoop(JsonObject node) {
            return node.getBoolean("loop", false);
        }

        // NotNull
        private String nameOf(JsonObject node) {
            String name = node.getString("name", null);
            if (name == null) {
                throw badConfiguration("node without name");
            }
            return name;
        }

        private void addLoopNode() {
            loops.add(Optional.ofNullable(nameStack.peek()).orElseThrow(
                    () -> badConfiguration("LOOP IN ROOT")));
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

        private List<String> namesOf(JsonArray children) {
            return children.stream() //
                    .map(JsonObject.class::cast) //
                    .map(this::walk) // recursion
                    .filter(Objects::nonNull) //
                    .collect(Collectors.toList());
        }

        private RuntimeException badConfiguration(String message) {
            Collections.reverse(nameStack); // side effect
            String path = String.join(".", nameStack);
            return new IllegalStateException(String.format("[%s] %s", path, message));
        }
    }
}