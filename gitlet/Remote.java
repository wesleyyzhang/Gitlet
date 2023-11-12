package gitlet;

import java.io.Serializable;
import java.util.TreeMap;

public class Remote implements Serializable {
    /** Remote constructor. */
    Remote() {
        _remotes = new TreeMap<>();
    }

    /** Gets the remotes.
     *
     * @return remotes
     * */
    public TreeMap<String, String> getRemote() {
        return this._remotes;
    }

    /** Remotes TreeMap. */
    private TreeMap<String, String> _remotes;
}
