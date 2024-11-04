package com.zeljko.abstractive.zsv.manager.utils

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths


object FileUtils {

    fun getObjectShaPath(path: Path, objectSha: String): Path {
        val repositoryPath = path.resolve(".git/objects/${objectSha.substring(0, 2)}/${objectSha.substring(2)}")

        if (!Files.exists(path)) {
            throw ObjectNotFoundException("Object not found.")
        }
        return repositoryPath
    }

    fun storeObject(directory: Path, objectSha: String, compressedContent: ByteArray) {
        val subDirectory = directory.resolve(objectSha.substring(0, 2))
        Files.createDirectories(subDirectory)
        val blobFile = subDirectory.resolve(objectSha.substring(2))
        Files.write(blobFile, compressedContent)
    }

    fun getCurrentPath(): Path {
        return Paths.get("").toAbsolutePath()
    }

    fun getCurrentHead(): String {
        val headDirectory = Paths.get(".git/HEAD")

        if (!Files.exists(headDirectory)) {
            throw ObjectNotFoundException("Not a git repository")
        }

        val headContent = Files.readString(headDirectory).trim()

        return if (headContent.startsWith("ref: ")) {
            val branchReference = headContent.substringAfter("ref: ").trim()
            val branchDirectory = Paths.get(".git/$branchReference")

            if (Files.exists(branchDirectory)) {
                Files.readString(branchDirectory).trim()
            } else {
                "" // New branch
            }
        } else {
            headContent
        }
    }

    fun updateCurrentHead(commitSha: String) {
        val headDirectory = Paths.get(".git/HEAD")
        val headContent = Files.readString(headDirectory)

        if (headContent.startsWith("ref: ")) {
            val branchReference = headContent.substringAfter("ref: ").trim()
            val branchPath = Paths.get(".git", branchReference)
            Files.createDirectories(branchPath.parent)
            Files.writeString(branchPath, commitSha + "\n")
            println("Updated branch reference: $branchPath with SHA: $commitSha")
        } else {
            Files.writeString(headDirectory, commitSha + "\n")
            println("Updated HEAD directly with SHA: $commitSha")
        }
    }

}