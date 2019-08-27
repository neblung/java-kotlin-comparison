package tree;

import java.util.Collection;
import java.util.List;

public interface Tree {
    // Name of the root node
    String getRoot();

    // Names of the children of the node with the given name
    List<String> getChildren(String parent);

    // Names of the nodes that are loop nodes
    Collection<String> getLoops();
}
