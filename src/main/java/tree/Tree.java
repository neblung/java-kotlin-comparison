package tree;

import java.util.Collection;
import java.util.List;

public interface Tree {
    String getRoot();

    List<String> getChildren(String parent);

    Collection<String> getLoops();
}
