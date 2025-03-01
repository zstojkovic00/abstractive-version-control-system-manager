package com.zeljko.abstractive.zsv.manager.command

import com.zeljko.abstractive.zsv.manager.transport.RemoteRepositoryClient
import org.springframework.shell.command.annotation.Command
import org.springframework.shell.command.annotation.Option


@Command(command = ["zsv"], description = "Zsv commands")
class RemoteRepositoryCommands(
    private val remoteRepositoryClient : RemoteRepositoryClient
) {


    @Command(command = ["push"], description = "Push to remote server")
    fun push(
        @Option(shortNames = ['b'], description = "Branch name") branchName: String = "master"
    ): String {
        return remoteRepositoryClient.push(branchName)
    }

    @Command(command = ["pull"], description = "Pull from remote server")
    fun pull(
        @Option(shortNames = ['b'], description = "Branch name") branchName: String = "master"
    ): String {
        return remoteRepositoryClient.pull(branchName)
    }
}