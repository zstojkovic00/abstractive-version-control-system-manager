package com.zeljko.abstractive.zsv.manager.utils

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths


object FileUtils {

    /**
    Resolves the path to a ZSV object file in the .zsv/objects directory

    @param objectSha The name of zsv object (SHA-1 hash)
     */
    fun getObjectShaPath(objectSha: String): Path {

        val path = Paths.get(".git/objects/${objectSha.substring(0, 2)}/${objectSha.substring(2)}")

        if (!Files.exists(path)) {
            throw ObjectNotFoundException("Object not found.")
        }
        return path
    }

    /**
    Stores object in the specified directory

    @param directory where the object will be stored
    @param objectSHA. The name of object (SHA-1 hash)
    @param compressedContent The compressed content of the object
     */

    fun storeObject(directory: Path, objectSha: String, compressedContent: ByteArray) {
        val subDirectory = directory.resolve(objectSha.substring(0, 2))
        Files.createDirectories(subDirectory)
        val blobFile = subDirectory.resolve(objectSha.substring(2))
        Files.write(blobFile, compressedContent)
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