package gitlet;

import java.io.File;
import java.io.IOException;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.List;
import java.util.Arrays;


/** Driver class for Gitlet, the tiny stupid version-control system.
 *  @author Wesley Zhang
 */
public class Main {
    /** Current Working Directory. */
    static final File CWD = new File(".");

    /** Main metadata folder. */
    static final File GITLET_FOLDER = Utils.join(CWD, ".gitlet");

    /** File that stores the Staging Area. */
    static final File STAGING = Utils.join(GITLET_FOLDER, "staging");

    /** File that stores the pointers. */
    static final File POINTER = Utils.join(GITLET_FOLDER, "pointer");

    /** Format for the dates of commits. */
    static final SimpleDateFormat FORMAT =
            new SimpleDateFormat("EEE MMM d HH:mm:ss yyyy Z");

    /** File that stores the remotes. */
    static final File REMOTE = Utils.join(GITLET_FOLDER, "remote");

    /** Usage: java gitlet.Main ARGS, where ARGS contains
     *  <COMMAND> <OPERAND> .... */
    public static void main(String... args) throws IOException {
        if (args.length == 0) {
            exitWithError("Please enter a command.");
        }
        if (!args[0].equals("init") && !GITLET_FOLDER.exists()) {
            exitWithError("Not in an initialized Gitlet directory.");
        }
        switch (args[0]) {
        case "":
            exitWithError("Please enter a command.");
        case "init":
            setUpPersistence();
            break;
        case "add":
            add(args[1]);
            break;
        case "commit":
            commit(args[1]);
            break;
        case "checkout":
            if (args.length == 2) {
                checkoutBranch(args);
            } else {
                checkout(args);
            }
            break;
        case "log":
            log();
            break;
        case "rm":
            remove(args[1]);
            break;
        case "global-log":
            globalLog();
            break;
        case "find":
            find(args[1]);
            break;
        case "branch":
            branch(args[1]);
            break;
        case "rm-branch":
            removeBranch(args[1]);
            break;
        case "status":
            status();
            break;
        case "reset":
            reset(args[1]);
            break;
        case "merge":
            merge(args[1]);
            break;
        default:
            main2(args);
        }
    }

    /** Extension of main.
     *
     * @param args arguments
     * */
    public static void main2(String... args) throws IOException {
        switch (args[0]) {
        case "add-remote":
            addRemote(args);
            break;
        case "rm-remote":
            rmRemote(args);
            break;
        case "push":
            push(args);
            break;
        case "fetch":
            fetch(args);
            break;
        case "pull":
            pull(args);
            break;
        default:
            exitWithError("No command with that name exists.");
        }
    }

    /** Pull command.
     * @param args arguments
     * */
    public static void pull(String... args) throws IOException {
        fetch(args);
        String branch = args[1] + "/" + args[2];
        merge(branch);
    }

    /** Push command.
     * @param args arguments
     */
    public static void push(String... args) {
        Remote remote = Utils.readObject(REMOTE, Remote.class);
        TreeMap<String, String> remotes = remote.getRemote();
        String dir = remotes.get(args[1]);
        File file1 = new File(dir);
        if (!file1.exists()) {
            exitWithError("Remote directory not found.");
        }
        File rPointerFile = Utils.join(dir, "pointer");
        Pointer remotePointer = Utils.readObject(rPointerFile, Pointer.class);
        File rBlobs = Utils.join(dir, "blobs");
        File rCommitFolder = Utils.join(dir, "commits");
        Pointer pointer = Utils.readObject(POINTER, Pointer.class);
        TreeMap<String, String> headName = pointer.getHeadname();
        String currBranch = headName.get("*");
        TreeMap<String, String> branches = pointer.getBranches();
        String currSha1 = branches.get(currBranch);
        Commit currCommit = Commit.fromFile(currSha1);
        TreeMap<String, String> blobs = currCommit.getBlobs();
        TreeMap<String, String> rBranches = remotePointer.getBranches();
        String remoteCommit = rBranches.get(args[2]);
        File curr = Utils.join(Commit.COMMIT_FOLDER, remoteCommit);
        if (!curr.exists()) {
            exitWithError("Please pull down remote changes before pushing.");
        }
        for (Map.Entry<String, String> entry : blobs.entrySet()) {
            String sha1 = entry.getValue();
            File blob = Utils.join(Commit.BLOB_FOLDER, sha1);
            String contents = Utils.readContentsAsString(blob);
            File newBlob = Utils.join(rBlobs, sha1);
            Utils.writeContents(newBlob, contents);
        }
        File newCommit = Utils.join(rCommitFolder, currSha1);
        Utils.writeObject(newCommit, currCommit);
        Pointer rPointer = Utils.readObject(rPointerFile, Pointer.class);
        TreeMap<String, String> remoteBranches = rPointer.getBranches();
        remoteBranches.replace(args[2], currSha1);
        TreeMap<String, String> remoteHead = rPointer.getHead();
        remoteHead.replace("*", currSha1);
        Utils.writeObject(rPointerFile, rPointer);
    }

    /** Fetch command.
     * @param args arguments
     * */
    public static void fetch(String... args) {
        Remote remote = Utils.readObject(REMOTE, Remote.class);
        TreeMap<String, String> remotes = remote.getRemote();
        String dir = remotes.get(args[1]);
        File file1 = new File(dir);
        if (!file1.exists()) {
            exitWithError("Remote directory not found.");
        }
        File rPointerFile = Utils.join(dir, "pointer");
        File rBlobs = Utils.join(dir, "blobs");
        Pointer remotePointer = Utils.readObject(rPointerFile, Pointer.class);
        TreeMap<String, String> branches = remotePointer.getBranches();
        if (!branches.containsKey(args[2])) {
            exitWithError("That remote does not have that branch.");
        }
        String remoteBranch = branches.get(args[2]);
        File rCommitFolder = Utils.join(dir, "commits");
        File file = Utils.join(rCommitFolder, remoteBranch);
        Commit remoteCommit = Utils.readObject(file, Commit.class);
        TreeMap<String, String> remoteBlobs = remoteCommit.getBlobs();
        for (Map.Entry<String, String> entry : remoteBlobs.entrySet()) {
            String sha1 = entry.getValue();
            File blob = Utils.join(rBlobs, sha1);
            String contents = Utils.readContentsAsString(blob);
            File newBlob = Utils.join(Commit.BLOB_FOLDER, sha1);
            Utils.writeContents(newBlob, contents);
        }
        File newCommit = Utils.join(Commit.COMMIT_FOLDER, remoteBranch);
        Utils.writeObject(newCommit, remoteCommit);
        Pointer pointer = Utils.readObject(POINTER, Pointer.class);
        TreeMap<String, String> localBranches = pointer.getBranches();
        localBranches.put(args[1] + "/" + args[2], remoteBranch);
        Utils.writeObject(POINTER, pointer);
    }

    /** Remove-remote command.
     * @param args arguments
     * */
    public static void rmRemote(String... args) {
        Remote remote = Utils.readObject(REMOTE, Remote.class);
        TreeMap<String, String> remotes = remote.getRemote();
        if (!remotes.containsKey(args[1])) {
            exitWithError("A remote with that name does not exist.");
        }
        remotes.remove(args[1]);
        Utils.writeObject(REMOTE, remote);
    }

    /** Add-remote command.
     * @param args arguments
     * */
    public static void addRemote(String... args) {
        Remote remote = Utils.readObject(REMOTE, Remote.class);
        TreeMap<String, String> remotes = remote.getRemote();
        if (remotes.containsKey(args[1])) {
            exitWithError("A remote with that name already exists.");
        }
        remotes.put(args[1], args[2]);
        Utils.writeObject(REMOTE, remote);
    }

    /** Gets the min value.
     *
     * @param array given int array
     * @return min value of array
     * */
    public static int getMin(int[] array) {
        int minValue = array[0];
        for (int i = 1; i < array.length; i++) {
            if (array[i] < minValue) {
                minValue = array[i];
            }
        }
        return minValue;
    }

    /** Finds split point.
     *
     * @param allParents linked hashmap
     * @param i counter
     * @return index
     * */
    public static int helper(LinkedHashMap<String, int[]> allParents, int i) {
        for (Map.Entry<String, int[]> entry : allParents.entrySet()) {
            int[] times = entry.getValue();
            if (times.length == 2) {
                break;
            }
            i += 1;
        }
        return i;
    }

    /** Fills up the LinkedHashMaps.
     *
     * @param currParents current parents list
     * @param givenParents given parents list
     * @param allParents all parents list
     * */
    public static void fillLinkedHash(ArrayList<String> currParents,
                                      ArrayList<String> givenParents,
                                      LinkedHashMap<String, int[]> allParents) {
        for (String sha1 : currParents) {
            if (!allParents.containsKey(sha1)) {
                allParents.put(sha1, new int[1]);
            }
        }
        for (String sha1 : givenParents) {
            if (!allParents.containsKey(sha1)) {
                allParents.put(sha1, new int[1]);
            } else {
                int[] i = allParents.get(sha1);
                int update = i.length + 1;
                allParents.put(sha1, new int[update]);
            }
        }
    }

    /** Fills ArrayLists.
     *
     * @param branch branch
     * @param currParents1 parent1 list
     * @param currParents2 parent2 list
     * @param givenParents1 given1 list
     * @param givenParents2 given2 list
     * */
    public static void fillArrayLists(String branch,
                                      ArrayList<String> currParents1,
                                      ArrayList<String> currParents2,
                                      ArrayList<String> givenParents1,
                                      ArrayList<String> givenParents2) {
        Pointer pointer = Utils.readObject(POINTER, Pointer.class);
        TreeMap<String, String> head = pointer.getHead();
        TreeMap<String, String> branches = pointer.getBranches();
        String currSha1 = head.get("*");
        String givenSha1 = branches.get(branch);
        Commit current = Commit.fromFile(currSha1);
        Commit given = Commit.fromFile(givenSha1);
        currParents1.add(currSha1);
        while (current.getParent() != null) {
            currParents1.add(current.getParent());
            String parentSha1 = current.getParent();
            current = Commit.fromFile(parentSha1);
        }
        current = Commit.fromFile(currSha1);
        currParents2.add(currSha1);
        while (current.getParent2() != null) {
            currParents2.add(current.getParent2());
            String parentSha1 = current.getParent2();
            current = Commit.fromFile(parentSha1);
        }
        givenParents1.add(givenSha1);
        while (given.getParent() != null) {
            givenParents1.add(given.getParent());
            String parentSha1 = given.getParent();
            given = Commit.fromFile(parentSha1);
        }
        given = Commit.fromFile(givenSha1);
        givenParents2.add(givenSha1);
        while (given.getParent2() != null) {
            givenParents2.add(given.getParent2());
            String parentSha1 = given.getParent2();
            given = Commit.fromFile(parentSha1);
        }
    }
    /** Returns the split point.
     * @param branch given branch
     * @return split point sha1
     * */
    public static String findSplit(String branch) {
        ArrayList<String> currParents1 = new ArrayList<>();
        ArrayList<String> given1 = new ArrayList<>();
        ArrayList<String> currParents2 = new ArrayList<>();
        ArrayList<String> given2 = new ArrayList<>();
        fillArrayLists(branch, currParents1, currParents2, given1, given2);
        LinkedHashMap<String, int[]> allParents11 = new LinkedHashMap<>();
        LinkedHashMap<String, int[]> allParents12 = new LinkedHashMap<>();
        LinkedHashMap<String, int[]> allParents21 = new LinkedHashMap<>();
        LinkedHashMap<String, int[]> allParents22 = new LinkedHashMap<>();
        fillLinkedHash(currParents1, given1, allParents11);
        fillLinkedHash(currParents1, given2, allParents12);
        fillLinkedHash(currParents2, given1, allParents21);
        fillLinkedHash(currParents2, given2, allParents22);
        String parent = "";
        int i11 = helper(allParents11, 0);
        int i12 = helper(allParents12, 0);
        int i21 = helper(allParents21, 0);
        int i22 = helper(allParents22, 0);
        int[] ints = new int[4];
        ints[0] = i11;
        ints[1] = i12;
        ints[2] = i21;
        ints[3] = i22;
        int min = getMin(ints);
        if (i11 == min) {
            for (Map.Entry<String, int[]> entry : allParents11.entrySet()) {
                if (i11 == 0) {
                    parent = entry.getKey();
                    break;
                }
                i11 -= 1;
            }
        } else if (i12 == min) {
            for (Map.Entry<String, int[]> entry : allParents12.entrySet()) {
                if (i12 == 0) {
                    parent = entry.getKey();
                    break;
                }
                i12 -= 1;
            }
        } else if (i21 == min) {
            for (Map.Entry<String, int[]> entry : allParents21.entrySet()) {
                if (i21 == 0) {
                    parent = entry.getKey();
                    break;
                }
                i21 -= 1;
            }
        } else if (i22 == min) {
            for (Map.Entry<String, int[]> entry : allParents22.entrySet()) {
                if (i22 == 0) {
                    parent = entry.getKey();
                    break;
                }
                i22 -= 1;
            }
        }
        return parent;
    }

    /** Fills the allfiles array.
     *
     * @param allFiles allfiles
     * @param currBlobs current blobs
     * @param givenBlobs given blobs
     * @param splitBlobs split point blobs
     */
    public static void fillArray(ArrayList<String> allFiles, TreeMap<String,
            String> currBlobs, TreeMap<String, String> givenBlobs,
                                 TreeMap<String, String> splitBlobs) {
        List<String> cwdFiles = Utils.plainFilenamesIn(CWD);
        for (String fileName : cwdFiles) {
            if (!currBlobs.containsKey(fileName)) {
                if (!givenBlobs.containsKey(fileName)) {
                    exitWithError("There is an untracked file in the way; "
                            + "delete it, or add and commit it first.");
                }
                File file = Utils.join(CWD, fileName);
                String fileContents = Utils.readContentsAsString(file);
                String fileSha1 = Utils.sha1(fileContents);
                String prevSha1 = givenBlobs.get(fileName);
                if (!fileSha1.equals(prevSha1)) {
                    exitWithError("There is an untracked file in the way; "
                            + "delete it, or add and commit it first.");
                }
            }
        }
        for (Map.Entry<String, String> entry : currBlobs.entrySet()) {
            if (!allFiles.contains(entry.getKey())) {
                allFiles.add(entry.getKey());
            }
        }
        for (Map.Entry<String, String> entry : givenBlobs.entrySet()) {
            if (!allFiles.contains(entry.getKey())) {
                allFiles.add(entry.getKey());
            }
        }
        for (Map.Entry<String, String> entry : splitBlobs.entrySet()) {
            if (!allFiles.contains(entry.getKey())) {
                allFiles.add(entry.getKey());
            }
        }
    }

    /** Merge cases.
     * @param branchSha1 branch Sha1 code
     * @param allFiles list of all files
     * @param currBlobs current blobs
     * @param givenBlobs given blobs
     * @param splitBlobs split blobs
     * @return checks whether there is a conflict
     */
    public static boolean merge2(String branchSha1, ArrayList<String> allFiles,
                              TreeMap<String, String> currBlobs,
                              TreeMap<String, String> givenBlobs,
                              TreeMap<String, String> splitBlobs)
            throws IOException {
        boolean conflict = false;
        for (String file : allFiles) {
            String sFile = splitBlobs.get(file);
            String cFile = currBlobs.get(file);
            String gFile = givenBlobs.get(file);
            if (sFile != null) {
                if (cFile != null && gFile != null) {
                    if (sFile.equals(cFile) && !sFile.equals(gFile)) {
                        helper3(branchSha1, file);
                    } else if ((!cFile.equals(gFile) && sFile.equals(cFile))
                            || (!cFile.equals(gFile) && !cFile.equals(sFile)
                                    && !gFile.equals(sFile))) {
                        helper2(file, cFile, gFile);
                        conflict = true;
                    }
                } else if (gFile == null && cFile != null) {
                    if (sFile.equals(cFile)) {
                        remove(file);
                    } else if (!cFile.equals(sFile)) {
                        File curr = Utils.join(Commit.BLOB_FOLDER, cFile);
                        String currString = Utils.readContentsAsString(curr);
                        File cwd = Utils.join(CWD, file);
                        Utils.writeContents(cwd, "<<<<<<< HEAD\n"
                                + currString + "=======\n>>>>>>>\n");
                        conflict = true;
                        add(file);
                    }
                } else if (cFile == null && gFile != null) {
                    if (!gFile.equals(sFile)) {
                        File given = Utils.join(Commit.BLOB_FOLDER, gFile);
                        String givenString = Utils.readContentsAsString(given);
                        File cwd = Utils.join(CWD, file);
                        Utils.writeContents(cwd, "<<<<<<< HEAD\n======="
                                + givenString + ">>>>>>>\n");
                        conflict = true;
                        add(file);
                    }
                }
            } else if (sFile == null) {
                if (cFile == null && gFile != null) {
                    helper3(branchSha1, file);
                } else if (cFile != null && gFile != null) {
                    if (!cFile.equals(gFile)) {
                        helper2(file, cFile, gFile);
                        conflict = true;
                    }
                }
            }
        }
        return conflict;
    }

    /** Helper3 for merge2.
     *
     * @param branchSha1 sha1 code of branch
     * @param file file name
     */
    public static void helper3(String branchSha1,
                               String file) throws IOException {
        String[] arg = new String[4];
        arg[0] = "checkout";
        arg[1] = branchSha1;
        arg[2] = "--";
        arg[3] = file;
        checkout(arg);
        add(file);
    }

    /** Helper2 for merge2.
     *
     * @param file file
     * @param currFile current file
     * @param givenFile given file
     */
    public static void helper2(String file, String currFile,
                               String givenFile) throws IOException {
        File curr = Utils.join(Commit.BLOB_FOLDER, currFile);
        String currString = Utils.readContentsAsString(curr);
        File given = Utils.join(Commit.BLOB_FOLDER, givenFile);
        String givenString = Utils.readContentsAsString(given);
        File cwd = Utils.join(CWD, file);
        Utils.writeContents(cwd, "<<<<<<< HEAD\n"
                + currString + "=======\n" + givenString + ">>>>>>>\n");
        add(file);
    }

    /** Merges files from the given branch into the current branch.
     * @param branch given branch
     * */
    public static void merge(String branch) throws IOException {
        StagingArea staging = Utils.readObject(STAGING, StagingArea.class);
        TreeMap<String, byte[]> add = staging.getAdd();
        TreeMap<String, byte[]> remove = staging.getRemove();
        if (!add.isEmpty() || !remove.isEmpty()) {
            exitWithError("You have uncommitted changes.");
        }
        Pointer pointer = Utils.readObject(POINTER, Pointer.class);
        TreeMap<String, String> branches = pointer.getBranches();
        String branchSha1 = branches.get(branch);
        if (branchSha1 == null) {
            exitWithError("A branch with that name does not exist.");
        }
        TreeMap<String, String> headName = pointer.getHeadname();
        TreeMap<String, String> head = pointer.getHead();
        String currBranch = headName.get("*");
        if (currBranch.equals(branch)) {
            exitWithError("Cannot merge a branch with itself.");
        }
        String splitSha1 = findSplit(branch);
        if (splitSha1.equals(branchSha1)) {
            System.out.println("Given branch is an ancestor "
                    + "of the current branch.");
            return;
        }
        String currSha1 = head.get("*");
        if (currSha1.equals(splitSha1)) {
            String[] arg = new String[2];
            arg[0] = "checkout";
            arg[1] = branch;
            checkoutBranch(arg);
            System.out.println("Current branch fast-forwarded.");
            return;
        }
        ArrayList<String> allFiles = new ArrayList<>();
        Commit currCommit = Commit.fromFile(currSha1);
        TreeMap<String, String> currBlobs = currCommit.getBlobs();
        Commit givenCommit = Commit.fromFile(branchSha1);
        TreeMap<String, String> givenBlobs = givenCommit.getBlobs();
        Commit split = Commit.fromFile(splitSha1);
        TreeMap<String, String> splitBlobs = split.getBlobs();
        fillArray(allFiles, currBlobs, givenBlobs, splitBlobs);
        boolean conflict = merge2(branchSha1, allFiles,
                currBlobs, givenBlobs, splitBlobs);
        staging = Utils.readObject(STAGING, StagingArea.class);
        commit("Merged " + branch + " into " + currBranch + ".");
        if (conflict) {
            System.out.println("Encountered a merge conflict.");
        }
        pointer = Utils.readObject(POINTER, Pointer.class);
        head = pointer.getHead();
        String newSha1 = head.get("*");
        Commit newCommit = Commit.fromFile(newSha1);
        newCommit.setParent2(branchSha1);
        String updateSha1 = newCommit.commitSha1();
        newCommit.saveCommit(updateSha1);
        head.replace("*", updateSha1);
        Utils.writeObject(POINTER, pointer);
    }

    /** Checks out all the files tracked by the given commit.
     * Removes tracked files that are not present in that commit.
     * Also moves the current branch's head to that commit node.
     *
     * @param id Commit id
     */
    public static void reset(String id) {
        File commitFile = Utils.join(Commit.COMMIT_FOLDER, id);
        if (!commitFile.exists()) {
            exitWithError("No commit with that id exists.");
        }
        Pointer pointer = Utils.readObject(POINTER, Pointer.class);
        StagingArea staging = Utils.readObject(STAGING, StagingArea.class);
        TreeMap<String, byte[]> add = staging.getAdd();
        TreeMap<String, byte[]> remove = staging.getRemove();
        TreeMap<String, String> headName = pointer.getHeadname();
        TreeMap<String, String> head = pointer.getHead();
        TreeMap<String, String> branches = pointer.getBranches();
        Commit commit = Commit.fromFile(id);
        TreeMap<String, String> blobs = commit.getBlobs();
        String currSha1 = head.get("*");
        Commit currCommit = Commit.fromFile(currSha1);
        TreeMap<String, String> currBlobs = currCommit.getBlobs();
        List<String> cwdFiles = Utils.plainFilenamesIn(CWD);
        for (String fileName : cwdFiles) {
            if (!currBlobs.containsKey(fileName)) {
                if (!blobs.containsKey(fileName)) {
                    break;
                }
                File file = Utils.join(CWD, fileName);
                String fileContents = Utils.readContentsAsString(file);
                String fileSha1 = Utils.sha1(fileContents);
                String prevSha1 = blobs.get(fileName);
                if (!fileSha1.equals(prevSha1)) {
                    exitWithError("There is an untracked file in the way; "
                            + "delete it, or add and commit it first.");
                }
            }
        }
        for (Map.Entry<String, String> entry : blobs.entrySet()) {
            String fileName = entry.getKey();
            String[] args = new String[4];
            args[0] = "checkout";
            args[1] = id;
            args[2] = "--";
            args[3] = fileName;
            checkout(args);
        }
        for (String fileName : cwdFiles) {
            if (!blobs.containsKey(fileName)) {
                File file = Utils.join(CWD, fileName);
                file.delete();
            }
        }
        String currBranchName = headName.get("*");
        head.replace("*", id);
        branches.replace(currBranchName, id);
        add.clear();
        remove.clear();
        Utils.writeObject(STAGING, staging);
        Utils.writeObject(POINTER, pointer);
    }

    /** Untracked. */
    public static void status2() {
        Pointer pointer = Utils.readObject(POINTER, Pointer.class);
        StagingArea staging = Utils.readObject(STAGING, StagingArea.class);
        TreeMap<String, byte[]> add = staging.getAdd();
        TreeMap<String, byte[]> remove = staging.getRemove();
        TreeMap<String, String> head = pointer.getHead();
        System.out.println("=== Modifications Not Staged For Commit ===");
        List<String> cwdFiles = Utils.plainFilenamesIn(CWD);
        String currSha1 = head.get("*");
        Commit currCommit = Commit.fromFile(currSha1);
        TreeMap<String, String> blobs = currCommit.getBlobs();
        for (Map.Entry<String, String> entry : blobs.entrySet()) {
            String fileName = entry.getKey();
            File file = Utils.join(CWD, fileName);
            if (!file.exists() && !remove.containsKey(fileName)) {
                System.out.println(fileName + " (deleted)");
            }
        }
        for (String fileName : cwdFiles) {
            if (blobs.containsKey(fileName)) {
                String blobSha1 = blobs.get(fileName);
                File blobFile = Utils.join(Commit.BLOB_FOLDER, blobSha1);
                File file = Utils.join(CWD, fileName);
                if (!file.exists() && !remove.containsKey(fileName)) {
                    System.out.println(fileName + " (deleted)");
                    continue;
                }
                String blobString = Utils.readContentsAsString(blobFile);
                String fileString = Utils.readContentsAsString(file);
                if (!blobString.equals(fileString)
                        && !add.containsKey(fileName)) {
                    System.out.println(fileName + " (modified)");
                    continue;
                }
            }
            if (add.containsKey(fileName)) {
                File file = Utils.join(CWD, fileName);
                if (!file.exists()) {
                    System.out.println(fileName + " (deleted)");
                    continue;
                }
                byte[] addContents = add.get(fileName);
                byte[] fileContents = Utils.readContents(file);
                if (!Arrays.equals(addContents, fileContents)) {
                    System.out.println(fileName + " (modified)");
                }
            }
        }
        System.out.println(" ");
        System.out.println("=== Untracked Files ===");
        for (String fileName : cwdFiles) {
            if (!blobs.containsKey(fileName) && !add.containsKey(fileName)) {
                System.out.println(fileName);
            } else if (remove.containsKey(fileName)) {
                System.out.println(fileName);
            }
        }
        System.out.println(" ");
    }

    /** Displays what branches currently exist,
     * and marks the current branch with a *.
     * */
    public static void status() {
        Pointer pointer = Utils.readObject(POINTER, Pointer.class);
        StagingArea staging = Utils.readObject(STAGING, StagingArea.class);
        TreeMap<String, byte[]> add = staging.getAdd();
        TreeMap<String, byte[]> remove = staging.getRemove();
        TreeMap<String, String> headName = pointer.getHeadname();
        TreeMap<String, String> head = pointer.getHead();
        String headBranch = headName.get("*");
        TreeMap<String, String> branches = pointer.getBranches();
        System.out.println("=== Branches ===");
        for (Map.Entry<String, String> entry : branches.entrySet()) {
            String branch = entry.getKey();
            if (branch.equals(headBranch)) {
                System.out.println("*" + branch);
            } else {
                System.out.println(branch);
            }
        }
        System.out.println(" ");
        System.out.println("=== Staged Files ===");
        for (Map.Entry<String, byte[]> entry : add.entrySet()) {
            String fileName = entry.getKey();
            System.out.println(fileName);
        }
        System.out.println(" ");
        System.out.println("=== Removed Files ===");
        for (Map.Entry<String, byte[]> entry : remove.entrySet()) {
            String fileName = entry.getKey();
            System.out.println(fileName);
        }
        System.out.println(" ");
        status2();
    }

    /** Deletes the branch with the given name.
     *
     * @param name branch name
     * */
    public static void removeBranch(String name) {
        Pointer pointer = Utils.readObject(POINTER, Pointer.class);
        TreeMap<String, String> branches = pointer.getBranches();
        TreeMap<String, String> headName = pointer.getHeadname();
        String currBranchName = headName.get("*");
        if (!branches.containsKey(name)) {
            exitWithError("A branch with that name does not exist.");
        } else if (currBranchName.equals(name)) {
            exitWithError("Cannot remove the current branch.");
        }
        branches.remove(name);
        Utils.writeObject(POINTER, pointer);
    }

    /** Creates a new branch with the given name,
     * and points it at the current head node.
     *
     * @param name branch name
     * */
    public static void branch(String name) {
        Pointer pointer = Utils.readObject(POINTER, Pointer.class);
        TreeMap<String, String> head = pointer.getHead();
        TreeMap<String, String> branches = pointer.getBranches();
        if (branches.containsKey(name)) {
            exitWithError("A branch with that name already exists.");
        }
        String currCommitCode = head.get("*");
        branches.put(name, currCommitCode);
        Utils.writeObject(POINTER, pointer);
    }

    /** Prints out the ids of all commits that
     * have the given commit message, one per line.
     *
     * @param message Commit message
     * */
    public static void find(String... message) {
        List<String> commits = Utils.plainFilenamesIn(Commit.COMMIT_FOLDER);
        int counter = 0;
        for (String commit : commits) {
            Commit current = Commit.fromFile(commit);
            String m = current.getMessage();
            if (m.equals(message[0])) {
                System.out.println(commit);
                counter += 1;
            }
        }
        if (counter == 0) {
            System.out.println("Found no commit with that message.");
        }
    }

    /** Displays information about all commits ever made. */
    public static void globalLog() throws IOException {
        List<String> commits = Utils.plainFilenamesIn(Commit.COMMIT_FOLDER);
        for (String commit : commits) {
            Commit current = Commit.fromFile(commit);
            String code = current.commitSha1();
            System.out.println("===");
            System.out.println("commit " + code);
            Timestamp time = current.getTime();
            System.out.println("Date: " + FORMAT.format(time));
            String message = current.getMessage();
            System.out.println(message);
            System.out.println(" ");
        }
    }

    /** Removes the file.
     *
     * @param fileName file's name
     * */
    public static void remove(String fileName) {
        StagingArea staging = Utils.readObject(STAGING, StagingArea.class);
        TreeMap<String, byte[]> add = staging.getAdd();
        if (add.containsKey(fileName)) {
            add.remove(fileName);
            Utils.writeObject(STAGING, staging);
        } else {
            Pointer pointer = Utils.readObject(POINTER, Pointer.class);
            TreeMap<String, String> head = pointer.getHead();
            String currCommit = head.get("*");
            Commit current = Commit.fromFile(currCommit);
            TreeMap<String, String> blobs = current.getBlobs();
            if (blobs.containsKey(fileName)) {
                staging.getRemove().put(fileName, null);
                File file = Utils.join(CWD, fileName);
                if (file.exists()) {
                    file.delete();
                }
            }
            if (!add.containsKey(fileName) && !blobs.containsKey(fileName)) {
                exitWithError("No reason to remove the file.");
            }
            Utils.writeObject(STAGING, staging);
            Utils.writeObject(POINTER, pointer);
        }
    }

    /** Logs the information. */
    public static void log() throws IOException {
        Pointer pointer = Utils.readObject(POINTER, Pointer.class);
        TreeMap<String, String> head = pointer.getHead();
        String currCommit = head.get("*");
        Commit current = Commit.fromFile(currCommit);
        while (current != null) {
            String code = current.commitSha1();
            System.out.println("===");
            System.out.println("commit " + code);
            Timestamp time = current.getTime();
            System.out.println("Date: " + FORMAT.format(time));
            String message = current.getMessage();
            System.out.println(message);
            String parent = current.getParent();
            if (parent == null) {
                return;
            }
            current = Commit.fromFile(parent);
            System.out.println(" ");
        }
    }

    /** Checks out a commit.
     *
     * @param args checkout arguments
     * */
    public static void checkout(String... args) {
        Pointer pointer = Utils.readObject(POINTER, Pointer.class);
        TreeMap<String, String> head = pointer.getHead();
        if (args[2].equals("++")) {
            exitWithError("Incorrect operands.");
        }
        if (args[1].equals("--")) {
            String currCommit = head.get("*");
            Commit current = Commit.fromFile(currCommit);
            TreeMap<String, String> blobs = current.getBlobs();
            if (!blobs.containsKey(args[2])) {
                exitWithError("File does not exist in that commit.");
            }
            String blobName = blobs.get(args[2]);
            File blobFile = Utils.join(Commit.BLOB_FOLDER, blobName);
            String blobContents = Utils.readContentsAsString(blobFile);
            File cwdFile = new File(args[2]);
            if (cwdFile.exists()) {
                Utils.writeContents(cwdFile, blobContents);
            } else {
                File newFile = Utils.join(CWD, args[2]);
                Utils.writeContents(newFile, blobContents);
            }
        } else if (args[2].equals("--")) {
            if (args[1].length() == 8) {
                List<String> commitIDs =
                        Utils.plainFilenamesIn(Commit.COMMIT_FOLDER);
                for (String id : commitIDs) {
                    if (id.startsWith(args[1])) {
                        args[1] = id;
                    }
                }
            }
            File commitFile = Utils.join(Commit.COMMIT_FOLDER, args[1]);
            if (!commitFile.exists()) {
                exitWithError("No commit with that id exists.");
            }
            Commit commit = Commit.fromFile(args[1]);
            TreeMap<String, String> blobs = commit.getBlobs();
            if (!blobs.containsKey(args[3])) {
                exitWithError("File does not exist in that commit.");
            }
            String blobName = blobs.get(args[3]);
            File blobFile = Utils.join(Commit.BLOB_FOLDER, blobName);
            byte[] blobContents = Utils.readContents(blobFile);
            File cwdFile = new File(args[3]);
            if (cwdFile.exists()) {
                Utils.writeContents(cwdFile, blobContents);
            } else {
                File newFile = Utils.join(CWD, args[3]);
                Utils.writeContents(newFile, blobContents);
            }
        }
    }

    /** Checkout a branch.
     *
     * @param args checkout's arguments
     * */
    public static void checkoutBranch(String... args) throws IOException {
        Pointer pointer = Utils.readObject(POINTER, Pointer.class);
        StagingArea staging = Utils.readObject(STAGING, StagingArea.class);
        TreeMap<String, byte[]> add = staging.getAdd();
        TreeMap<String, byte[]> remove = staging.getRemove();
        TreeMap<String, String> branches = pointer.getBranches();
        TreeMap<String, String> headName = pointer.getHeadname();
        TreeMap<String, String> head = pointer.getHead();
        String currBranchName = headName.get("*");
        if (!branches.containsKey(args[1])) {
            exitWithError("No such branch exists.");
        } else if (currBranchName.equals(args[1])) {
            exitWithError("No need to checkout the current branch.");
        }
        String branchCode = branches.get(args[1]);
        Commit branch = Commit.fromFile(branchCode);
        String currCommitCode = head.get("*");
        Commit currCommit = Commit.fromFile(currCommitCode);
        TreeMap<String, String> currBlobs = currCommit.getBlobs();
        TreeMap<String, String> branchBlobs = branch.getBlobs();
        List<String> cwdFiles = Utils.plainFilenamesIn(CWD);
        for (String fileName : cwdFiles) {
            if (!currBlobs.containsKey(fileName)) {
                if (!branchBlobs.containsKey(fileName)) {
                    exitWithError("There is an untracked file in the way; "
                            + "delete it, or add and commit it first.");
                }
                File file = Utils.join(CWD, fileName);
                String fileContents = Utils.readContentsAsString(file);
                String fileSha1 = Utils.sha1(fileContents);
                String prevSha1 = branchBlobs.get(fileName);
                if (!fileSha1.equals(prevSha1)) {
                    exitWithError("There is an untracked file in the way; "
                            + "delete it, or add and commit it first.");
                }
            }
        }
        for (String fileName : cwdFiles) {
            File file = Utils.join(CWD, fileName);
            file.delete();
        }
        for (Map.Entry<String, String> entry : branchBlobs.entrySet()) {
            String fileName = entry.getKey();
            File file = Utils.join(CWD, fileName);
            file.createNewFile();
            String sha1 = entry.getValue();
            File blob = Utils.join(Commit.BLOB_FOLDER, sha1);
            String blobContents = Utils.readContentsAsString(blob);
            Utils.writeContents(file, blobContents);
        }
        add.clear();
        remove.clear();
        headName.replace("*", args[1]);
        String sha1 = branches.get(args[1]);
        head.replace("*", sha1);
        Utils.writeObject(STAGING, staging);
        Utils.writeObject(POINTER, pointer);
    }


    /** Creates a new commit.
     *
     * @param message Commit's message
     * */
    public static void commit(String message) throws IOException {
        StagingArea staging = Utils.readObject(STAGING, StagingArea.class);
        TreeMap<String, byte[]> add = staging.getAdd();
        TreeMap<String, byte[]> remove = staging.getRemove();
        if (add.isEmpty() && remove.isEmpty()) {
            exitWithError("No changes added to the commit.");
        }
        if (message.equals("")) {
            exitWithError("Please enter a commit message.");
        }
        Pointer pointer = Utils.readObject(POINTER, Pointer.class);

        TreeMap<String, String> head = pointer.getHead();
        TreeMap<String, String> headname = pointer.getHeadname();
        TreeMap<String, String> branches = pointer.getBranches();

        String parentName = head.get("*");
        Commit parent = Commit.fromFile(parentName);
        Commit current = new Commit(message, parentName);
        current.getBlobs().putAll(parent.getBlobs());
        for (Map.Entry<String, byte[]> entry : add.entrySet()) {
            String key = entry.getKey();
            byte[] value = entry.getValue();
            String sha1 = Utils.sha1(value);
            current.getBlobs().put(key, sha1);
        }
        for (Map.Entry<String, byte[]> entry : remove.entrySet()) {
            String key = entry.getKey();
            current.getBlobs().remove(key);
        }
        String code = current.commitSha1();
        current.saveCommit(code);
        add.clear();
        remove.clear();
        head.replace("*", code);
        String currBranch = headname.get("*");
        branches.replace(currBranch, code);
        Utils.writeObject(STAGING, staging);
        Utils.writeObject(POINTER, pointer);
    }


    /** Prints out MESSAGE and exits with error code 0.
     * @param message message to print.
     */
    public static void exitWithError(String message) {
        if (message != null && !message.equals("")) {
            System.out.println(message);
        }
        System.exit(0);
    }


    /** Creates any necessary files and folders for persistance. */
    public static void setUpPersistence() throws IOException {
        if (GITLET_FOLDER.exists()) {
            exitWithError(" A Gitlet version-control "
                    + "system already exists in the current directory.");
        }
        if (!GITLET_FOLDER.exists()) {
            GITLET_FOLDER.mkdir();
        }
        if (!Commit.COMMIT_FOLDER.exists()) {
            Commit.COMMIT_FOLDER.mkdir();
        }
        if (!Commit.BLOB_FOLDER.exists()) {
            Commit.BLOB_FOLDER.mkdir();
        }
        if (!STAGING.exists()) {
            STAGING.createNewFile();
            StagingArea staging = new StagingArea();
            Utils.writeObject(STAGING, staging);
        }
        if (!POINTER.exists()) {
            POINTER.createNewFile();
        }
        if (!REMOTE.exists()) {
            REMOTE.createNewFile();
            Remote remote = new Remote();
            Utils.writeObject(REMOTE, remote);
        }
        Commit init = new Commit("initial commit", null);
        Timestamp unix = new Timestamp(0);
        init.setTime(unix);
        String code = init.commitSha1();
        init.saveCommit(code);
        Pointer pointer = new Pointer();
        pointer.getHead().put("*", code);
        pointer.getHeadname().put("*", "master");
        pointer.getBranches().put("master", code);
        Utils.writeObject(POINTER, pointer);
    }


    /** Adds file to the staging area.
     *
     * @param fileName name of file
     * */
    public static void add(String fileName) throws IOException {
        Pointer pointer = Utils.readObject(POINTER, Pointer.class);
        StagingArea staging = Utils.readObject(STAGING, StagingArea.class);
        TreeMap<String, byte[]> add = staging.getAdd();
        TreeMap<String, byte[]> remove = staging.getRemove();
        File file = new File(fileName);
        if (!file.exists()) {
            exitWithError("File does not exist.");
        } else if (remove.containsKey(fileName)) {
            remove.remove(fileName);
        } else {
            if (add.containsKey(fileName)) {
                add.replace(fileName, Utils.readContents(file));
            } else {
                add.put(fileName, Utils.readContents(file));
            }
            TreeMap<String, String> head = pointer.getHead();
            String current = head.get("*");
            Commit parent = Commit.fromFile(current);
            TreeMap<String, String> blobs = parent.getBlobs();
            if (blobs.containsKey(fileName)) {
                String sha1 = blobs.get(fileName);
                File blob = Utils.join(Commit.BLOB_FOLDER, sha1);
                byte[] blobByte = Utils.readContents(blob);
                byte[] currentByte = Utils.readContents(file);
                if (Arrays.equals(blobByte, currentByte)) {
                    if (add.containsKey(fileName)) {
                        add.remove(fileName);
                    }
                    if (remove.containsKey(fileName)) {
                        remove.remove(fileName);
                    }
                }
            }
        }
        for (Map.Entry<String, byte[]> entry : add.entrySet()) {
            byte[] value = entry.getValue();
            String sha1 = Utils.sha1(value);
            File newBlob = Utils.join(Commit.BLOB_FOLDER, sha1);
            newBlob.createNewFile();
            Utils.writeContents(newBlob, value);
        }
        Utils.writeObject(STAGING, staging);
    }


}
