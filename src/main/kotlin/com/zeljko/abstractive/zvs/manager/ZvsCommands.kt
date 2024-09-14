package com.zeljko.abstractive.zvs.manager

import org.springframework.shell.command.annotation.Command

import java.io.File

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

            listOf("objects", "refs/heads", "refs/tags").forEach() {
                File(zvsDirectory, it).mkdirs()
            }

            File(zvsDirectory, "HEAD").writeText("ref: refs/heads/master\n")
            File(zvsDirectory, "description").writeText("Unnamed repository; edit this file 'description' to name the repository.\n")

            return "Initialized empty Zvs repository in $currentDirectory/.zvs/"
        } catch (e: Exception) {
            return "Error initializing Zvs repository: ${e.message}"
        }
    }
}