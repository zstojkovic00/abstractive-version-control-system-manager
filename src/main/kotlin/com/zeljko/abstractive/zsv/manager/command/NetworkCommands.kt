package com.zeljko.abstractive.zsv.manager.command

import com.zeljko.abstractive.zsv.manager.transport.client.GitClient
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.shell.command.annotation.Command
import org.springframework.shell.command.annotation.Option


@Command(command = ["zsv"], description = "Zsv commands")
class NetworkCommands(
    @Qualifier("native") private val nativeClient: GitClient,
    @Qualifier("http") private val httpClient: GitClient
) {

    // zsv clone git://127.0.0.1/test-repo
    @Command(command = ["clone"], description = "Clone remote repository from git server")
    fun cloneRepository(
        @Option(longNames = ["url"], required = true, description = "Url of remote git repository") url: String,
    ): String {
        val client = when {
            url.startsWith("git://") -> nativeClient
            url.startsWith("http://") || url.startsWith("https://") -> httpClient
            else -> throw IllegalArgumentException("Unsupported protocol")
        }

        client.clone(url)
        return "test"
    }
}