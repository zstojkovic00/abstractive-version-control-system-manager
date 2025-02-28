package com.zeljko.abstractive.zsv.manager.command

import com.zeljko.abstractive.zsv.manager.transport.client.MinioService
import org.springframework.shell.command.annotation.Command
import org.springframework.shell.command.annotation.Option


@Command(command = ["zsv"], description = "Zsv commands")
class NetworkCommands(
    private val minioService: MinioService
) {


    @Command(command = ["push"], description = "Push to remote server")
    fun push(
        @Option(shortNames = ['b'], required = false, description = "Branch name") branchName: String = "master"
    ): String {
        return minioService.push(branchName)
    }

    @Command(command = ["pull"], description = "Pull from remote server")
    fun pull(
        @Option(shortNames = ['b'], required = false, description = "Branch name") branchName: String = "master"
    ): String {
        return minioService.pull(branchName)
    }
}