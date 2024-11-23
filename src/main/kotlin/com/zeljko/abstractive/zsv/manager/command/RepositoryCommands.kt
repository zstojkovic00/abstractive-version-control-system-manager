package com.zeljko.abstractive.zsv.manager.command

import com.zeljko.abstractive.zsv.manager.core.services.CheckoutService
import com.zeljko.abstractive.zsv.manager.core.services.IndexService
import com.zeljko.abstractive.zsv.manager.core.services.RepositoryService
import com.zeljko.abstractive.zsv.manager.utils.FileUtils.ZSV_DIR
import com.zeljko.abstractive.zsv.manager.utils.FileUtils.createZsvStructure
import org.springframework.shell.command.annotation.Command
import org.springframework.shell.command.annotation.Option


@Command(command = ["zsv"], description = "Zsv commands")
class RepositoryCommands(
    private val checkoutService: CheckoutService,
    private val indexService: IndexService,
    private val repositoryService: RepositoryService
) {
    @Command(command = ["init"], description = "Initialize empty $ZSV_DIR repository")
    fun initRepository(): String {
        val zsvPath = createZsvStructure()
        return "Initialized empty zsv repository in $zsvPath"
    }

    @Command(command = ["status"], description = "prints current branch, untracked files, changes to be committed")
    fun status() {
        repositoryService.status()
    }

    @Command(command = ["add"], description = "Add file to staging area")
    fun writeFileToIndex(
        @Option(required = true, description = "Path to file") filePath: String
    ) {
        indexService.saveFileToIndex(filePath)
    }

    @Command(command = ["cat-index"], description = "Read index file")
    fun readIndexFile(
    ) {
        println(indexService.parseIndexFile())
    }

    @Command(command = ["checkout"], description = "Switch to existing branch")
    fun checkout(
        @Option(required = true, description = "Branch name to checkout") branchName: String
    ): String {
        checkoutService.checkout(branchName, isNewBranch = false)
        return "Switched to branch $branchName"
    }

    @Command(command = ["checkout -b"], description = "Create and switch to new branch")
    fun checkoutNewBranch(
        @Option(required = true, description = "New branch name") branchName: String
    ): String {
        checkoutService.checkout(branchName, isNewBranch = true)
        return "Switched to a new branch $branchName"
    }

}


