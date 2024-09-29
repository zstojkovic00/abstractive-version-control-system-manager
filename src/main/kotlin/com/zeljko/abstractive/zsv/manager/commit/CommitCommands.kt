package com.zeljko.abstractive.zsv.manager.commit

import org.springframework.shell.command.annotation.Command
import org.springframework.shell.command.annotation.Option


@Command(command = ["zsv"], description = "Zsv commands")
class CommitCommands(private val commitService: CommitService) {


    // zsv commit-tree
    @Command(command = ["commit-tree"], description = "Commit tree")
    fun commitTree(@Option(shortNames = ['m'], required = true, description = "Message of commit") message: String,
                   @Option(shortNames = ['t'], required = true, description = "SHA-1 of write-tree command") treeSha: String,
                   @Option(shortNames = ['p'], required = false, description = "SHA-1 of parent commit if there is one") parentSha: String
    ): String {
        return commitService.commitTree(message, treeSha, parentSha)
    }


}

