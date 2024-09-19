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
    fun initRepository(): String {

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
            File(
                zsvDirectory, "description"
            ).writeText("Unnamed repository; edit this file 'description' to name the repository.\n")

            return "Initialized empty zsv repository in $currentDirectory/.zsv/"
        } catch (e: Exception) {
            return "Error initializing zsv repository: ${e.message}"
        }
    }


    // zsv cat-file -p
    @Command(command = ["cat-file"], description = "Read blob object")
    fun decompressBlobObject(
        @Option(shortNames = ['p'], required = false, description = "pretty print") prettyPrint: Boolean,
        @Option(shortNames = ['f'], required = true, description = "Path to the file to decompress") blobSha: String
    ): String {
        if (blobSha.length != 40) {
            return "Error: Invalid object hash. It must be exactly 40 characters long."
        }

        val path = getHashObjectPath(blobSha)

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
        @Option(
            shortNames = ['w'],
            required = false,
            description = "When used with the -w flag, it also writes the object to the .zsv/objects directory"
        ) write: Boolean = false, @Option(
            shortNames = ['f'], required = true, description = "Path to the file to compress"
        ) fileToCompress: String
    ): String {
        val currentDirectory: String = System.getProperty("user.dir")
        val path: Path = Paths.get(fileToCompress)

        val content: String = Files.readString(path)
        val blobContent = "blob ${content.length}\u0000$content"
        val compressedContent: ByteArray = blobContent.zlibCompress()

        // create blob name
        val blobNameSHA1: String = blobContent.toSha1()

        crateBlobDirectory(File(currentDirectory), compressedContent, blobNameSHA1)

        if (write) {
            val objectsDirectory = File(currentDirectory, ".zsv/objects")
            crateBlobDirectory(objectsDirectory, compressedContent, blobNameSHA1)
        }
        return blobNameSHA1
    }


    @Command(command = ["ls-tree"], description = "Read tree object")
    fun decompressTreeObject(
        @Option(longNames = ["name-only"], required = true, description = "When used with --name-only flag, it only prints name of file") nameOnly: Boolean,
        @Option(shortNames = ['f'], required = false, description = "Path to directory you want to decompress") treeSha: String
    ): String {

        val path = getHashObjectPathTest(treeSha)

        val compressedContent: ByteArray = Files.readAllBytes(path)
        val decompressedContent = compressedContent.zlibDecompress()

        if (!decompressedContent.startsWith("tree")) {
            return "Error: Not a tree object."
        }
        println(decompressedContent)

        val trees = parseTreeContent(decompressedContent)


        return if (nameOnly) {
            trees.joinToString("\n") { it.fileName }
        } else {
            trees.joinToString("\n") { tree ->
                "${tree.fileMode} " +
                        "${if (tree.fileMode.startsWith("040")) "tree" else "blob"} " +
                        "${tree.objectSha}\t${tree.fileName}"
            }
        }
    }

    // 100644 (regular file)
    // 100766 (executable file)
    // 120000 (symbolic link)
    // 040000 (directory)

    // directory    object       hash                                        name
    // 040000       tree         4c2de5dff69543f68b6238e0510d420b59b334f7    main

    private fun parseTreeContent(decompressedContent: String): List<Tree> {

        // remove <tree> <size>
        val content = decompressedContent.split("\u0000").drop(1).joinToString("")
        var index = 0

        while(index < content.length){
            val nextTreeIndex = content.indexOf("40000", index + 1)

        }

        return listOf(Tree(
            fileMode = "0",
            objectSha = "0",
            fileName = "0"
        ))
    }


    private fun crateBlobDirectory(directory: File, compressedContent: ByteArray, blobName: String) {
        val subDirectory = File(directory, blobName.substring(0, 2))
        subDirectory.mkdirs()
        val newFile = File(subDirectory, blobName.substring(2))
        newFile.writeBytes(compressedContent)
    }

    private fun getHashObjectPath(objectHashToDecompress: String): Path {
        val path = Paths.get(".zsv/objects/${objectHashToDecompress.substring(0, 2)}/${objectHashToDecompress.substring(2)}")
        return path
    }

    private fun getHashObjectPathTest(objectHashToDecompress: String): Path {
        val path = Paths.get(".git/objects/${objectHashToDecompress.substring(0, 2)}/${objectHashToDecompress.substring(2)}")
        return path
    }
}


