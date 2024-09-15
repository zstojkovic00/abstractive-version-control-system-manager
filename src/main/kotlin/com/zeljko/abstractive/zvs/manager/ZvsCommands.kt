package com.zeljko.abstractive.zvs.manager

import org.springframework.shell.command.annotation.Command
import org.springframework.shell.command.annotation.Option

import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.security.MessageDigest

@Command(command = ["zvs"], description = "Zvs commands")
class ZvsCommands {

    @Command(command = ["init"], description = "Initialize empty .zvs repository")
    fun init(): String {

        val currentDirectory = System.getProperty("user.dir")
        val zvsDirectory = File(currentDirectory, ".zvs");

        if (zvsDirectory.exists()) {
            return "Zvs repository already exists in this directory";
        }

        try {
            zvsDirectory.mkdir()

            listOf("objects", "refs/heads", "refs/tags").forEach {
                //file -> File(zvsDirectory, file).mkdirs()
                File(zvsDirectory, it).mkdirs()
            }

            File(zvsDirectory, "HEAD").writeText("ref: refs/heads/master\n")
            File(zvsDirectory, "description").writeText("Unnamed repository; edit this file 'description' to name the repository.\n")

            return "Initialized empty Zvs repository in $currentDirectory/.zvs/"
        } catch (e: Exception) {
            return "Error initializing Zvs repository: ${e.message}"
        }
    }


    // zsv cat-file -p
    @Command(command = ["cat-file"], description = "Read blob object")
    fun decompressBlobObject(@Option(shortNames = ['p'], required = true, description = "pretty print") prettyPrint: Boolean,
                             @Option(description = "Compressed file you want to read") objectHashToDecompress: String
    ): String {
        if (objectHashToDecompress.length != 40) {
            return "Error: Invalid object hash. It must be exactly 40 characters long."
        }

        val path = Paths.get(".zvs/objects/${objectHashToDecompress.substring(0, 2)}/${objectHashToDecompress.substring(2)}")

        if (!Files.exists(path)) {
            return "Error: Object not found."
        }

        val compressedContent = Files.readAllBytes(path)
        val decompressedContent = compressedContent.zlibDecompress()

        if (!decompressedContent.startsWith("blob")) {
            return "Only blob objects are currently supported."
        }

        return decompressedContent
    }


    // zsv hash-object -w test.txt
    @Command(command = ["hash-object"], description = "Create a blob object")
    fun compressFileToBlobObject(
            @Option(shortNames = ['w'], required = false, description = "When used with the -w flag, it also writes the object to the .zvs/objects directory") write: Boolean = false,
            @Option(shortNames = ['f'], required = true, description = "Path to the file to compress") fileToCompress: String
    ): String {
        val currentDirectory: String = System.getProperty("user.dir")
        val path: Path = Paths.get(fileToCompress)

        val content: String = Files.readString(path)
        val compressedContent: ByteArray = content.zlibCompress()

        // create blob name
        val blobNameSHA1: String = ("blob ${content.length}\u0000$content").toSha1()

        crateBlobDirectory(File(currentDirectory), compressedContent, blobNameSHA1)

        if (write) {
            val objectsDirectory = File(currentDirectory, ".zvs/objects")
            crateBlobDirectory(objectsDirectory, compressedContent, blobNameSHA1)
        }
        return blobNameSHA1
    }

    private fun crateBlobDirectory(directory: File, compressedContent: ByteArray, blobName: String) {
        val subDirectory = File(directory, blobName.substring(0, 2))
        subDirectory.mkdirs()
        val newFile = File(subDirectory, blobName.substring(2))
        newFile.writeBytes(compressedContent)
    }
}