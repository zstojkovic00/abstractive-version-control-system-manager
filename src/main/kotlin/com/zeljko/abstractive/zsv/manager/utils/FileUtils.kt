package com.zeljko.abstractive.zsv.manager.utils

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths


object FileUtils {

    private const val GIT_DIR = ".git"
    private const val OBJECTS_DIR = "$GIT_DIR/objects"
    private const val REFS_DIR = "$GIT_DIR/refs"
    private const val HEAD_FILE = "$GIT_DIR/HEAD"
    private const val HEADS_DIR = "$REFS_DIR/heads"

    fun getObjectShaPath(path: Path, objectSha: String): Path {
        val objectPath = path.resolve("$OBJECTS_DIR/${objectSha.substring(0, 2)}/${objectSha.substring(2)}")

        if (!Files.exists(objectPath)) {
            throw ObjectNotFoundException("Object not found.")
        }
        return objectPath
    }

    fun storeObject(directory: Path, objectSha: String, compressedContent: ByteArray) {
        val subDirectory = directory.resolve(objectSha.substring(0, 2))
        Files.createDirectories(subDirectory)
        val blobFile = subDirectory.resolve(objectSha.substring(2))
        Files.write(blobFile, compressedContent)
    }

    fun getCurrentPath(): Path = Paths.get("").toAbsolutePath()

    fun getGitDir(): Path {
        val currentPath = getCurrentPath()
        val gitPath = currentPath.resolve(GIT_DIR)

        if (Files.exists(gitPath)) {
            throw RepositoryAlreadyExistsException("zsv repository already exists in this directory")
        }

        return gitPath
    }

    fun getCurrentHead(): String {
        val headPath = Paths.get(HEAD_FILE)

        if (!Files.exists(headPath)) {
            throw ObjectNotFoundException("Not a git repository")
        }

        val headContent = Files.readString(headPath).trim()

        return if (headContent.startsWith("ref: ")) {
            val branchReference = headContent.substringAfter("ref: ").trim()
            val branchPath = Paths.get("$GIT_DIR/$branchReference")

            if (Files.exists(branchPath)) {
                Files.readString(branchPath).trim()
            } else {
                "" // New branch
            }
        } else {
            headContent
        }
    }

    fun updateCurrentHead(commitSha: String) {
        val headPath = Paths.get(HEAD_FILE)
        val headContent = Files.readString(headPath)

        if (headContent.startsWith("ref: ")) {
            val branchReference = headContent.substringAfter("ref: ").trim()
            val branchPath = Paths.get(GIT_DIR, branchReference)
            Files.createDirectories(branchPath.parent)
            Files.writeString(branchPath, commitSha + "\n")
            println("Updated branch reference: $branchPath with SHA: $commitSha")
        } else {
            Files.writeString(headPath, commitSha + "\n")
            println("Updated HEAD directly with SHA: $commitSha")
        }
    }

    fun readCommitShaFromBranchName(branchName: String): String {
        val branchPath = Paths.get("$HEADS_DIR/$branchName")
        return Files.readString(branchPath).trim()
    }

    fun checkIfBranchExists(branchName: String): Boolean {
        return Files.exists(Paths.get("$HEADS_DIR/$branchName"))
    }

    fun createNewBranch(branchName: String, commitSha: String) {
        val branchPath = Paths.get("$HEADS_DIR/$branchName")
        Files.createDirectories(branchPath.parent)
        Files.writeString(branchPath, "$commitSha\n")
    }
}