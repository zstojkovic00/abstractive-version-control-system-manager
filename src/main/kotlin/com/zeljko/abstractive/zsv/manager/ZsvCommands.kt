package com.zeljko.abstractive.zsv.manager

import com.zeljko.abstractive.zsv.manager.protocol.GitNativeProtocolClient
import com.zeljko.abstractive.zsv.manager.protocol.GitUrl
import com.zeljko.abstractive.zsv.manager.utils.RepositoryAlreadyExistsException
import org.springframework.shell.command.annotation.Command
import org.springframework.shell.command.annotation.Option

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

    // zsv clone git://127.0.0.1/test-repo
    @Command(command = ["clone"], description = "Clone remote repository from git server")
    fun cloneRepository(
        @Option(longNames = ["url"], required = true, description = "Url of remote git repository") url: String,
    ): String {

        val urlWithoutProtocol = url.removePrefix("git://")
        // parts[0] = 127.0.0.1, parts[1] = test-repo
        val parts = urlWithoutProtocol.split("/", limit = 2)

        val gitUrl = GitUrl(
            host = parts[0],
            port = 9418,
            path = "/${parts[1]}"
        )

        val client = GitNativeProtocolClient()
        client.connect(gitUrl)

        return "test"
    }
}


