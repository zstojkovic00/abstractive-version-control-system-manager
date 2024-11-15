package com.zeljko.abstractive.zsv.manager.command

import com.zeljko.abstractive.zsv.manager.core.services.CheckoutService
import com.zeljko.abstractive.zsv.manager.utils.FileUtils.getGitDir
import org.springframework.shell.command.annotation.Command
import org.springframework.shell.command.annotation.Option

import java.nio.charset.StandardCharsets
import java.nio.file.Files

@Command(command = ["zsv"], description = "Zsv commands")
class RepositoryCommands(
    private val checkoutService: CheckoutService
) {
    @Command(command = ["init"], description = "Initialize empty .zsv repository")
    fun initRepository(): String {
        val zsvPath = getGitDir()

        listOf("objects", "refs/heads", "refs/tags").forEach {
            Files.createDirectories(zsvPath.resolve(it))
        }

        Files.writeString(zsvPath.resolve("HEAD"), "ref: refs/heads/master\n", StandardCharsets.UTF_8)
        Files.writeString(
            zsvPath.resolve("description"),
            "Unnamed repository; edit this file 'description' to name the repository.\n", StandardCharsets.UTF_8
        )

        return "Initialized empty zsv repository in $zsvPath/.git/"
    }

    @Command(command = ["checkout"], description = "Switch to existing branch")
    fun checkout(
        @Option(required = true, description = "Branch name to checkout")
        branchName: String
    ): String {
        checkoutService.checkout(branchName, isNewBranch = false)
        return "Switched to branch '$branchName'"
    }

    @Command(command = ["checkout -b"], description = "Create and switch to new branch")
    fun checkoutNewBranch(
        @Option(required = true, description = "New branch name")
        branchName: String
    ): String {
        checkoutService.checkout(branchName, isNewBranch = true)
        return "Switched to a new branch '$branchName'"
    }

}


