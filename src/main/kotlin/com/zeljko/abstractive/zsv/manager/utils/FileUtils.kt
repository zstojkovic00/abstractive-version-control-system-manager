package com.zeljko.abstractive.zsv.manager.utils

import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths


object FileUtils {

    const val ZSV_DIR = ".zsv"
    const val OBJECTS_DIR = "$ZSV_DIR/objects"
    const val REFS_DIR = "$ZSV_DIR/refs"
    const val HEAD_FILE = "$ZSV_DIR/HEAD"
    const val HEADS_DIR = "$REFS_DIR/heads"

    fun createZsvStructure(path: Path = getCurrentPath()): Path {
        val zsvPath = path.resolve(ZSV_DIR)

        listOf("objects", "refs/heads", "refs/tags").forEach {
            Files.createDirectories(zsvPath.resolve(it))
        }

        Files.writeString(zsvPath.resolve("HEAD"), "ref: refs/heads/master\n", StandardCharsets.UTF_8)
        Files.writeString(
            zsvPath.resolve("description"),
            "Unnamed repository; edit this file 'description' to name the repository.\n", StandardCharsets.UTF_8
        )
        return zsvPath
    }

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

    fun getZsvDir(): Path {
        val currentPath = getCurrentPath()
        val zsvPath = currentPath.resolve(ZSV_DIR)

        if (Files.exists(zsvPath)) {
            throw RepositoryAlreadyExistsException("zsv repository already exists in this directory")
        }

        return zsvPath
    }

    fun getCurrentHead(): String {
        val headPath = Paths.get(HEAD_FILE)

        if (!Files.exists(headPath)) {
            throw ObjectNotFoundException("Not a zsv repository")
        }

        val headContent = Files.readString(headPath).trim()

        return if (headContent.startsWith("ref: ")) {
            val branchReference = headContent.substringAfter("ref: ").trim()
            val branchPath = Paths.get("$ZSV_DIR/$branchReference")

            if (Files.exists(branchPath)) {
                Files.readString(branchPath).trim()
            } else {
                "" // New branch
            }
        } else {
            headContent
        }
    }

    fun updateBranchCommit(commitSha: String) {
        val headPath = Paths.get(HEAD_FILE)
        val headContent = Files.readString(headPath)

        if (headContent.startsWith("ref: ")) {
            val branchReference = headContent.substringAfter("ref: ").trim()
            val branchPath = Paths.get(ZSV_DIR, branchReference)
            Files.createDirectories(branchPath.parent)
            Files.writeString(branchPath, commitSha + "\n")
            println("Updated branch reference: $branchPath with SHA: $commitSha")
        } else {
            Files.writeString(headPath, commitSha + "\n")
            println("Updated HEAD directly with SHA: $commitSha")
        }
    }

    fun updateHeadReference(branchName: String) {
        val headPath = Paths.get(HEAD_FILE)
        Files.writeString(headPath, "ref: refs/heads/$branchName\n")
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