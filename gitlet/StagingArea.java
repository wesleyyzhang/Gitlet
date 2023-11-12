package gitlet;

import java.io.Serializable;
import java.util.TreeMap;

public class StagingArea implements Serializable {
    /** StagingArea constructor.*/
    StagingArea() {
        _add = new TreeMap<>();
        _remove = new TreeMap<>();
    }

    /** Get the add treemap.
     *
     * @return add treemap
     * */
    public TreeMap<String, byte[]> getAdd() {
        return this._add;
    }

    /** Get the remove treemap.
     *
     * @return remove treemap
     * */
    public TreeMap<String, byte[]> getRemove() {
        return this._remove;
    }

    /** Add treemap.*/
    private TreeMap<String, byte[]> _add;

    /** Remove treemap.*/
    private TreeMap<String, byte[]> _remove;
}


