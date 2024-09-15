package com.zeljko.abstractive.zsv.manager

import org.springframework.shell.command.annotation.Command
import org.springframework.shell.command.annotation.Option

import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

@Command(command = ["zsv"], description = "Zsv commands")
class ZsvCommands {

    @Command(command = ["init"], description = "Initialize empty .zsv repository")
    fun init(): String {

        val currentDirectory = System.getProperty("user.dir")
        val zsvDirectory = File(currentDirectory, ".zsv");

        if (zsvDirectory.exists()) {
            return "zsv repository already exists in this directory";
        }

        try {
            zsvDirectory.mkdir()

            listOf("objects", "refs/heads", "refs/tags").forEach {
                //file -> File(zsvDirectory, file).mkdirs()
                File(zsvDirectory, it).mkdirs()
            }

            File(zsvDirectory, "HEAD").writeText("ref: refs/heads/master\n")
            File(zsvDirectory, "description").writeText("Unnamed repository; edit this file 'description' to name the repository.\n")

            return "Initialized empty zsv repository in $currentDirectory/.zsv/"
        } catch (e: Exception) {
            return "Error initializing zsv repository: ${e.message}"
        }
    }


    // zsv cat-file -p
    @Command(command = ["cat-file"], description = "Read blob object")
    fun decompressBlobObject(
            @Option(shortNames = ['p'], required = true, description = "pretty print") prettyPrint: Boolean,
            @Option(shortNames = ['f'], required = true, description = "Path to the file to decompress") objectHashToDecompress: String
    ): String {
        if (objectHashToDecompress.length != 40) {
            return "Error: Invalid object hash. It must be exactly 40 characters long."
        }

        val path = Paths.get(".zsv/objects/${objectHashToDecompress.substring(0, 2)}/${objectHashToDecompress.substring(2)}")

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
            @Option(shortNames = ['w'], required = false, description = "When used with the -w flag, it also writes the object to the .zsv/objects directory") write: Boolean = false,
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
            val objectsDirectory = File(currentDirectory, ".zsv/objects")
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