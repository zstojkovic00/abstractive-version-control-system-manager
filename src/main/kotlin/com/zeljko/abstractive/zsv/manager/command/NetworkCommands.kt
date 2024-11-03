package com.zeljko.abstractive.zsv.manager.command

import com.zeljko.abstractive.zsv.manager.transport.client.GitNativeClient
import com.zeljko.abstractive.zsv.manager.transport.model.GitUrl
import org.springframework.shell.command.annotation.Command
import org.springframework.shell.command.annotation.Option


@Command(command = ["zsv"], description = "Zsv commands")
class NetworkCommands(private val gitClient: GitNativeClient) {

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

        gitClient.clone(gitUrl)

        return "test"
    }
}