package com.zeljko.abstractive.zsv.manager.command

import com.zeljko.abstractive.zsv.manager.core.services.CheckoutService
import com.zeljko.abstractive.zsv.manager.utils.RepositoryAlreadyExistsException
import org.springframework.shell.command.annotation.Command
import org.springframework.shell.command.annotation.Option

import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Paths

@Command(command = ["zsv"], description = "Zsv commands")
class RepositoryCommands(
    private val checkoutService : CheckoutService
) {

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

    @Command(command = ["checkout"], description = "switch between branches")
    fun checkout(
        @Option(shortNames = ['b'], required = false, description = "Create and checkout new branch") newBranch: String?,
        @Option(required = false, description = "Branch name or commit hash to checkout") branchName: String?
    ) : String {
        if (newBranch != null) {
            checkoutService.checkout(branchName = null, newBranch = newBranch)
            return "Switched to a new branch '$newBranch'"
        } else if (branchName != null) {
            checkoutService.checkout(branchName = branchName, newBranch = null)
            return "Switched to branch '$branchName'"
        } else {
            throw IllegalArgumentException("Either -b new-branch or branch/commit must be specified")
        }
    }

}


