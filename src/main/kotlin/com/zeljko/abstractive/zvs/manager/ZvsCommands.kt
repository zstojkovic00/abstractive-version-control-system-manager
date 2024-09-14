package com.zeljko.abstractive.zvs.manager

import org.springframework.shell.command.annotation.Command
import org.springframework.shell.command.annotation.Option

import java.io.File
import java.nio.file.Files
import java.nio.file.Paths

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
//                file -> File(zvsDirectory, file).mkdirs()
                File(zvsDirectory, it).mkdirs()
            }

            File(zvsDirectory, "HEAD").writeText("ref: refs/heads/master\n")
            File(zvsDirectory, "description").writeText("Unnamed repository; edit this file 'description' to name the repository.\n")

            return "Initialized empty Zvs repository in $currentDirectory/.zvs/"
        } catch (e: Exception) {
            return "Error initializing Zvs repository: ${e.message}"
        }
    }

    @Command(command = ["cat-file"], description = "Read blob object")
    fun decompressBlobObject(@Option(shortNames = ['p'], required = true, description = "pretty print") objectHash: String): String {

        if (objectHash.length != 40) {
            return "Error: Invalid object hash. It must be exactly 40 characters long."
        }

        val path = Paths.get(".git/objects/${objectHash.substring(0, 2)}/${objectHash.substring(2)}")

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
}