package gitlet;

import java.io.Serializable;
import java.util.TreeMap;

public class Pointer implements Serializable {
    /** Pointer constructor. */
    Pointer() {
        _head = new TreeMap<>();
        _headname = new TreeMap<>();
        _branches = new TreeMap<>();
    }

    /** Getter for head.
     *
     * @return head treemap
     * */
    public TreeMap<String, String> getHead() {
        return this._head;
    }

    /** Getter for headname.
     *
     * @return headname treemap
     * */
    public TreeMap<String, String> getHeadname() {
        return this._headname;
    }

    /** Getter for branches.
     *
     * @return branches treemap
     * */
    public TreeMap<String, String> getBranches() {
        return this._branches;
    }

    /** Head pointer treemap. */
    private TreeMap<String, String> _head;

    /** Headname pointer treemap. */
    private TreeMap<String, String> _headname;

    /** Branches pointer treemap. */
    private TreeMap<String, String> _branches;

}
