package com.zeljko.abstractive.zsv.manager

import com.zeljko.abstractive.zsv.manager.core.services.BlobService
import com.zeljko.abstractive.zsv.manager.core.services.BranchService
import com.zeljko.abstractive.zsv.manager.core.services.CommitService
import com.zeljko.abstractive.zsv.manager.core.services.IndexService
import com.zeljko.abstractive.zsv.manager.core.services.RepositoryService
import com.zeljko.abstractive.zsv.manager.core.services.TreeService
import com.zeljko.abstractive.zsv.manager.transport.client.GitClient
import com.zeljko.abstractive.zsv.manager.utils.FileUtils.ZSV_DIR
import com.zeljko.abstractive.zsv.manager.utils.FileUtils.createZsvStructure
import com.zeljko.abstractive.zsv.manager.utils.FileUtils.getCurrentPath
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.shell.command.annotation.Command
import org.springframework.shell.command.annotation.Option
import java.nio.file.Files
import java.nio.file.Paths


@Command
class ZsvCommands(
    private val branchService: BranchService,
    private val indexService: IndexService,
    private val repositoryService: RepositoryService,
    private val commitService: CommitService,
    private val blobService: BlobService,
    private val treeService: TreeService,
    @param:Qualifier("native") private val nativeClient: GitClient,
    @param:Qualifier("http") private val httpClient: GitClient,
) {

    @Command(command = ["init"], description = "Initialize empty $ZSV_DIR repository")
    fun initRepository(): String {
        if (Files.exists(Paths.get(ZSV_DIR))) {
            return "Zsv repository already exist"
        }

        val zsvPath = createZsvStructure()
        return "Initialized empty zsv repository in $zsvPath"
    }

    @Command(command = ["branch"], description = "Prints current branch")
    fun getCurrentBranch(): String {
        return branchService.getCurrentBranchName()
    }

    @Command(command = ["status"], description = "prints current branch, untracked files, changes to be committed")
    fun status(): String {
        return repositoryService.status()
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
        branchService.checkout(branchName, isNewBranch = false)
        return "Switched to branch $branchName"
    }

    @Command(command = ["checkout -b"], description = "Create and switch to new branch")
    fun checkoutNewBranch(
        @Option(required = true, description = "New branch name") branchName: String
    ): String {
        branchService.checkout(branchName, isNewBranch = true)
        return "Switched to a new branch $branchName"
    }

    @Command(command = ["merge"], description = "Incorporates changes from the named commits into the current branch")
    fun merge(
        @Option(required = true, description = "New branch name") targetBranch: String
    ) {
        branchService.merge(targetBranch)
    }

    // zsv commit-tree
    @Command(command = ["commit-tree"], description = "Commit tree")
    fun commitTree(
        @Option(shortNames = ['m'], required = true, description = "Message of commit") message: String,
        @Option(shortNames = ['t'], required = true, description = "SHA-1 of write-tree command") treeSha: String,
        @Option(shortNames = ['p'], required = false, description = "SHA-1 of parent commit if there is one") parentSha: String
    ): String {
        return commitService.compressFromMessage(message, treeSha, parentSha)
    }

    @Command(command = ["commit"], description = "Commit")
    fun commit(
        @Option(shortNames = ['m'], required = true, description = "Message of commit") message: String
    ) {
        return commitService.commit(message)
    }

    @Command(command = ["log"], description = "Log all commits")
    fun log() {
        return commitService.readCommitRecursively()
    }

    // zsv cat-file -f a3c241ab148df99bc5924738958c3aaad76a322b
    @Command(command = ["cat-file"], description = "Read blob object")
    fun decompressBlobObject(
        @Option(shortNames = ['f'], required = true, description = "Path to the file to decompress") sha: String
    ): String {
        val blob = blobService.decompress(sha, getCurrentPath()).toString()


        // remove header (blob content.length)
        return blob.substringAfter('\u0000')
    }

    // zsv hash-object -w -f src/test.txt
    @Command(command = ["hash-object"], description = "Create blob object")
    fun compressFileToBlobObject(
        @Option(shortNames = ['w'], required = false, description = "When used with the -w flag, it also writes the object to the .zsv/objects directory") write: Boolean = false,
        @Option(shortNames = ['f'], required = true, description = "Path to the file to compress") fileToCompress: String
    ): String {

        val path = Paths.get(fileToCompress)
        return blobService.compressFromFile(write, path)
    }

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
