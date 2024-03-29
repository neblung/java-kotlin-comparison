package javatree;

import tree.Tree;

import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonValue;
import java.util.*;
import java.util.stream.Collectors;

public class JavaTree implements Tree {
    private final String root;
    private final Map<String, List<String>> childNames;
    private final Collection<String> loops;

    private JavaTree(String root, Map<String, List<String>> childNames, Collection<String> loops) {
        this.root = root;
        this.childNames = childNames;
        this.loops = loops;
    }

    public static JavaTree parse(JsonObject jsonTree) {
        return new Builder().build(jsonTree.getJsonObject("root"));
    }

    @Override
    public String getRoot() {
        return root;
    }

    @Override
    public List<String> getChildren(String parent) {
        return childNames.getOrDefault(parent, Collections.emptyList());
    }

    @Override
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
        private void addLoopNode() {
            loops.add(Optional.ofNullable(nameStack.peek()).orElseThrow(() -> badConfiguration("LOOP IN ROOT")));
        }

        private String nameOf(JsonObject node) {
            String name = node.getString("name", null);
            return Optional.ofNullable(name).orElseThrow(() -> badConfiguration("node without name"));
        }

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
            throw new IllegalStateException(String.format("[%s] %s", path, message));
        }
    }
}