package run;

import javatree.JavaTree;
import kotlintree.KotlinTreeKt;
import tree.Tree;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

public class Main {
    public static void main(String[] args) throws IOException {
        JsonObject sample = readSample();

        System.out.println("using java tree");
        useTree(JavaTree.parse(sample));

        System.out.println("\nusing kotlin tree");
        useTree(KotlinTreeKt.parse(sample));
    }

    private static JsonObject readSample() throws IOException {
        try (InputStream stream = Main.class.getResourceAsStream("/sample.json")) {
            try (JsonReader reader = Json.createReader(stream)) {
                return reader.readObject();
            }
        }
    }

    private static void useTree(Tree tree) {
        System.out.println("root == " + tree.getRoot());
        System.out.println("loops == " + tree.getLoops());
        printChildren(tree, tree.getRoot());
    }

    private static void printChildren(Tree tree, String node) {
        List<String> children = tree.getChildren(node);
        System.out.println("children(" + node + ") == " + children);
        // recursion
        for (String child : children) {
            printChildren(tree, child);
        }
    }
}
