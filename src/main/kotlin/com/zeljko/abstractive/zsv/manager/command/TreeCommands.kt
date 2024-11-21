package com.zeljko.abstractive.zsv.manager.command

import com.zeljko.abstractive.zsv.manager.core.services.TreeService
import com.zeljko.abstractive.zsv.manager.utils.FileUtils.getCurrentPath
import org.springframework.shell.command.annotation.Command
import org.springframework.shell.command.annotation.Option
import java.nio.file.Paths

@Command(command = ["zsv"], description = "Zsv commands")
class TreeCommands(private val treeService: TreeService) {

    // zsv ls-tree -f 54c4a4a636839e36be577bce569a8030d6d5354c
    @Command(command = ["ls-tree"], description = "Read tree object")
    fun decompressTreeObject(
        @Option(longNames = ["name-only"], required = false, description = "When used with --name-only flag, it only prints name of file") nameOnly: Boolean = false,
        @Option(shortNames = ['f'], required = true, description = "Path to directory you want to decompress") sha: String
    ): String {

        val trees = treeService.decompress(nameOnly, sha)
        return if (nameOnly) {
            trees.joinToString("\n") { it.fileName }
        } else {
            trees.joinToString("\n") { it.toString() }
        }
    }

    // zsv write-tree -> 5f4a3b5cec8f56436aef85c2304c5a02b5675c2e
    // git write-tree -> 5f4a3b5cec8f56436aef85c2304c5a02b5675c2e
    @Command(command = ["write-tree"], description = "Create tree object")
    fun compressTreeObject(): String {
        return treeService.compressFromFile()
    }
}