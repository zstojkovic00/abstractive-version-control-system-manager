package com.zeljko.abstractive.zsv.manager

import com.zeljko.abstractive.zsv.manager.utils.RepositoryAlreadyExistsException
import org.springframework.shell.command.annotation.Command

import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Paths

@Command(command = ["zsv"], description = "Zsv commands")
class ZsvCommands {

    @Command(command = ["init"], description = "Initialize empty .zsv repository")
    fun initRepository(): String {
        val currentDirectory = Paths.get("").toAbsolutePath()
        val zsvDirectory = currentDirectory.resolve(".zsv")

        if (Files.exists(zsvDirectory)) {
            throw RepositoryAlreadyExistsException("zsv repository already exists in this directory")
        }

        Files.createDirectory(zsvDirectory)

        listOf("objects", "refs/heads", "refs/tags").forEach {
            Files.createDirectories(zsvDirectory.resolve(it))
        }

        Files.writeString(zsvDirectory.resolve("HEAD"), "ref: refs/heads/master\n", StandardCharsets.UTF_8)
        Files.writeString(zsvDirectory.resolve("description"),
            "Unnamed repository; edit this file 'description' to name the repository.\n", StandardCharsets.UTF_8)

        return "Initialized empty zsv repository in $currentDirectory/.zsv/"
    }
}


