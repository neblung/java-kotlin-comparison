package run;

import toy.KToy;
import toy.KToyKt;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

public class Main {
    public static void main(String[] args) throws IOException {
        JsonObject sample = readSample();
//        JToy toy = JToy.parse(sample);
        KToy toy = KToyKt.parseToy(sample);
        System.out.println("root == " + toy.getRoot());
        System.out.println("loops == " + toy.getLoops());
        printChildren(toy, toy.getRoot());
    }

    //    private static void printChildren(JToy toy, String node) {
    private static void printChildren(KToy toy, String node) {
        List<String> children = toy.getChildren(node);
        System.out.println("children(" + node + ") == " + children);
        // recursion
        for (String child : children) {
            printChildren(toy, child);
        }

    }

    private static JsonObject readSample() throws IOException {
        try (InputStream stream = Main.class.getResourceAsStream("/sample.json")) {
            try (JsonReader reader = Json.createReader(stream)) {
                return reader.readObject();
            }
        }
    }
}
