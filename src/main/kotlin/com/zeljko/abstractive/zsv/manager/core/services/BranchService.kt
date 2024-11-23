package com.zeljko.abstractive.zsv.manager.core.services

import com.zeljko.abstractive.zsv.manager.utils.FileUtils
import com.zeljko.abstractive.zsv.manager.utils.FileUtils.ZSV_DIR
import com.zeljko.abstractive.zsv.manager.utils.FileUtils.getCurrentHead
import com.zeljko.abstractive.zsv.manager.utils.FileUtils.getCurrentPath
import com.zeljko.abstractive.zsv.manager.utils.FileUtils.updateHeadReference
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Service
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths


@Service
class BranchService(
    private val commitService: CommitService,
    private val treeService: TreeService
) {

    @EventListener
    fun updateBranchCommit(commitSha: String) {
        val headPath = Paths.get(FileUtils.HEAD_FILE)
        val headContent = Files.readString(headPath)

        if (headContent.startsWith("ref: ")) {
            val branchReference = headContent.substringAfter("ref: ").trim()
            val branchPath = Paths.get(ZSV_DIR, branchReference)
            Files.createDirectories(branchPath.parent)
            Files.writeString(branchPath, commitSha + "\n")
            println("Updated branch reference: $branchPath with SHA: $commitSha")
        } else {
            Files.writeString(headPath, commitSha + "\n")
            println("Updated HEAD directly with SHA: $commitSha")
        }
    }

    // TODO: support checkout with commitSha
    fun checkout(branchName: String, isNewBranch: Boolean) {
        if (!validateCheckout(branchName)) {
            throw IllegalStateException("Already on branch $branchName")
        }

        if (isNewBranch) {
            createAndCheckoutBranch(branchName)
        } else {
            checkoutExistingBranch(branchName)
        }
    }

    private fun checkoutExistingBranch(branchName: String) {
        val currentPath = getCurrentPath()

        if (!checkIfBranchExists(branchName)) {
            throw IllegalArgumentException("There is no branch $branchName")
        }

        val commitSha = readCommitShaFromBranchName(branchName)
        println(commitSha)
        cleanWorkingDirectory(currentPath)

        val (treeSha, _) = commitService.decompress(commitSha, currentPath)
        val decompressedTree = treeService.getDecompressedTreeContent(treeSha, currentPath)
        println(decompressedTree)
        treeService.extractToDisk(decompressedTree, treeSha, currentPath, currentPath)
        updateHeadReference(branchName)
    }

    private fun validateCheckout(branchName: String): Boolean {
        val currentBranch = getCurrentBranchName()
        return currentBranch != branchName
    }


    private fun createAndCheckoutBranch(branchName: String) {
        if (checkIfBranchExists(branchName)) {
            throw IllegalArgumentException("Branch $branchName already exists, use checkout without -b")
        }

        val currentCommitSha = getCurrentHead()
        createNewBranch(branchName, currentCommitSha)
        updateHeadReference(branchName)
    }

    private fun cleanWorkingDirectory(path: Path) {
        Files.walk(path)
            .filter { !it.startsWith(path.resolve(ZSV_DIR)) }
            .filter { it != path }
            .sorted(Comparator.reverseOrder())
            .forEach { Files.delete(it) }
    }

    fun getCurrentBranchName(): String {
        val branchName = Files.readString(Paths.get(FileUtils.HEAD_FILE)).trim()
        return if (branchName.startsWith("ref: ")) {
            branchName.substringAfter("refs: refs/heads/").trim()
        } else {
            "HEAD" // detached HEAD state
        }
    }

    fun createNewBranch(branchName: String, commitSha: String) {
        val branchPath = Paths.get("${FileUtils.HEADS_DIR}/$branchName")
        Files.createDirectories(branchPath.parent)
        Files.writeString(branchPath, "$commitSha\n")
    }

    fun readCommitShaFromBranchName(branchName: String): String {
        val branchPath = Paths.get("${FileUtils.HEADS_DIR}/$branchName")
        return Files.readString(branchPath).trim()
    }

    fun checkIfBranchExists(branchName: String): Boolean {
        return Files.exists(Paths.get("${FileUtils.HEADS_DIR}/$branchName"))
    }

}
