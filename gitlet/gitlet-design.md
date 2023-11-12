# Gitlet Design Document
author: Wesley Zhang

## 1. Classes and Data Structures

###Commit
This class stores information about when each commit was created and each parent 
commit. It also stores information about each file it contains. 

####Fields 
* String Timestamp: A variable storing when the commit was created. 
* String Parent: A string that stores the name/hashcode of the parent commit. 
* Tree Blobs: A tree of blobs of the commit.
* String Message: The message corresponding to the commit.

###Blob
This class stores the information/data of each file.

####Fields
* String Hashcode: A string that contains tha SHA1 Hashcode for this blob.
* String Filename: A string that stores the file name of the blob's 
corresponding file. 
* String Text: The text of the .txt file the blob represents. 
* ByteStream Serial: A stream of bytes that represents that object that 
the file contains. 

###Tree
This class contains the blobs of each commit. 

####Fields
* Node Root: The root node of the tree.

###Node
This class is the building block of the Tree structure.
####Fields
* Blob blob: A blob. 


###Tags
This class points to the current commit. 

####Fields
* Commit commit: The current commit. 

###StagingArea
This class represents the staging area. 
####Fields
* File Add: The add file area for the staging area.
* File Remove: The remove file area for the staging area. 

## 2. Algorithms

###Commit
* Commit(String Parent, Tree Blobs, String Message): The class constructor.
* SaveCommit(): Saves the commit and its contents into a file and stores it 
in the commit folder.
* ReadCommit(): Reads the contents of a commit file and converts it back 
into a commit. 

###Blob
* Blob(String Hashcode, String Filename, String Text, ByteStream Serial): 
The class constructor.

###StagingArea
* Remove(): Removes the files from the staging area. If the file isn't tracked 
by the commit, prints error message. 
* Add(): Adds the file to the staging area only if the contents of the file
are different from the version in the commit. 
* CreateCommit(): Creates a new commit from the files in the staging area. 

###Tags
* Checkout(): Reverts back to a previous state. 


## 3. Persistence
The .gitlet directory contains the subdirectories of commit and staging. The
staging directory contains subdirectories of add and remove. Files to be added will
have their names ad

Utils.writeObject will be used to serialize objects. 
* Add: When adding a file to the staging area, the file name is stored in the 
add subdirectory. A blob of the contents of the file will 
be created. Next, when commit is called, the blob will 
be added to the new commit.
* Merge: When merging commits, the correct files from each commit
will be added to the new commit. 
* Checkout: Every commit will be serialized and placed into the commit file. 
Therefore, every commit will be tracked in files. 


## 4. Design Diagram



