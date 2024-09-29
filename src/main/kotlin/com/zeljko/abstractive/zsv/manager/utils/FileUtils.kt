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

}