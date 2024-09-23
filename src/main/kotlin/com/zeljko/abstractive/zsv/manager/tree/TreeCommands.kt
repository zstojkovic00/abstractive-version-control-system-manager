package com.zeljko.abstractive.zsv.manager.tree

import org.springframework.shell.command.annotation.Command
import org.springframework.shell.command.annotation.Option
import org.springframework.stereotype.Component
import java.nio.file.Paths


@Component
@Command(command = ["zsv"], description = "Zsv commands")
class TreeCommands(private val treeService: TreeService) {


    // zsv ls-tree -f 54c4a4a636839e36be577bce569a8030d6d5354c
    @Command(command = ["ls-tree"], description = "Read tree object")
    fun decompressTreeObject(
        @Option(longNames = ["name-only"], required = false, description = "When used with --name-only flag, it only prints name of file") nameOnly: Boolean = false,
        @Option(shortNames = ['f'], required = true, description = "Path to directory you want to decompress") treeSha: String
    ): String {

        val trees = treeService.decompressTreeObject(nameOnly, treeSha)
        return if (nameOnly) {
            trees.joinToString("\n") { it.fileName }
        } else {
            trees.joinToString("\n") { it.toString() }
        }
    }
    @Command(command = ["write-tree"], description = "Create tree object")
    fun compressTreeObject(): String {
        return treeService.compressTreeObject(Paths.get("gradle").toAbsolutePath())
    }
}