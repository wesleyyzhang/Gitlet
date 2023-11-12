package gitlet;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.sql.Timestamp;
import java.util.TreeMap;

public class Commit implements Serializable {
    /** Commits folder. */
    static final File COMMIT_FOLDER = Utils.join(Main.GITLET_FOLDER, "commits");

    /** Blobs folder. */
    static final File BLOB_FOLDER = Utils.join(Main.GITLET_FOLDER, "blobs");

    /** Class constructor.
     *
     * @param message Commit message
     * @param parent Commit's parent
     * */
    public Commit(String message, String parent) {
        _time = new Timestamp(System.currentTimeMillis());
        _message = message;
        _parent = parent;
        _parent2 = parent;
        _blobs = new TreeMap<>();
    }

    /**
     * Reads in and deserializes a commit from a file
     * with name NAME in COMMIT_FOLDER.
     * If a commit with name passed in doesn't exist,
     * throw IllegalArgumentException error.
     *
     * @param name Name of commit to load
     * @return Commit read from file
     */
    public static Commit fromFile(String name) {
        File file = Utils.join(COMMIT_FOLDER, name);
        if (!file.exists()) {
            throw new IllegalArgumentException();
        }
        Commit commit = Utils.readObject(file, Commit.class);
        return commit;
    }

    /** Return sha1 code of commit.
     * @return Commit's sha1 code
     * */
    public String commitSha1() throws IOException {
        File initDummy = new File("name");
        initDummy.createNewFile();
        Utils.writeObject(initDummy, this);
        byte[] initByte = Utils.readContents(initDummy);
        String code = Utils.sha1(initByte);
        initDummy.delete();
        return code;
    }

    /** Saves a commit to a file for future use.
     *
     * @param sha1 Commit's Sha1 code
     * */
    public void saveCommit(String sha1) throws IOException {
        File commitFile = Utils.join(COMMIT_FOLDER, sha1);
        commitFile.createNewFile();
        Utils.writeObject(commitFile, this);
    }

    /** Gets the parent of the commit.
     * @return Commit's first parent
     * */
    public String getParent() {
        return this._parent;
    }

    /** Gets the second parent of the commit.
     * @return Commit's second parent
     * */
    public String getParent2() {
        return this._parent2;
    }

    /** Sets the parent.
     *
     * @param parent Commit's parent
     */
    public void setParent(String parent) {
        this._parent = parent;
    }

    /** Sets the parent2.
     * @param parent2 Commit's second parent
     * */
    public void setParent2(String parent2) {
        this._parent2 = parent2;
    }

    /** Gets the message of the commit.
     * @return Commit's message
     * */
    public String getMessage() {
        return this._message;
    }

    /** Gets the timestamp of the commit.
     * @return Commit's timestamp
     * */
    public Timestamp getTime() {
        return this._time;
    }

    /** Sets the time of the commit.
     * @param time Timestamp
     * */
    public void setTime(Timestamp time) {
        this._time = time;
    }

    /** Gets the blobs of the current commit.
     * @return Commit's blobs
     * */
    public TreeMap<String, String> getBlobs() {
        return this._blobs;
    }

    /** Sets the blobs of the current commit.
     * @param blob Commit's blobs
     * */
    public void setBlobs(TreeMap<String, String> blob) {
        this._blobs = blob;
    }

    /** Timestamp of commit. */
    private Timestamp _time;

    /** Commit message. */
    private String _message;

    /** Parent of commit. */
    private String _parent;

    /** Second parent. */
    private String _parent2;

    /** TreeMap of the commit's blobs. */
    private TreeMap<String, String> _blobs;


}
