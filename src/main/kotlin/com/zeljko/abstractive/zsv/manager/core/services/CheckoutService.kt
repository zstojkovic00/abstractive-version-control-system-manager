package com.zeljko.abstractive.zsv.manager.core.services

import com.zeljko.abstractive.zsv.manager.utils.FileUtils.ZSV_DIR
import com.zeljko.abstractive.zsv.manager.utils.FileUtils.checkIfBranchExists
import com.zeljko.abstractive.zsv.manager.utils.FileUtils.createNewBranch
import com.zeljko.abstractive.zsv.manager.utils.FileUtils.getCurrentBranchName
import com.zeljko.abstractive.zsv.manager.utils.FileUtils.getCurrentHead
import com.zeljko.abstractive.zsv.manager.utils.FileUtils.getCurrentPath
import com.zeljko.abstractive.zsv.manager.utils.FileUtils.readCommitShaFromBranchName
import com.zeljko.abstractive.zsv.manager.utils.FileUtils.updateHeadReference
import org.springframework.stereotype.Service
import java.nio.file.Files
import java.nio.file.Path


@Service
class CheckoutService(
    private val commitService: CommitService,
    private val treeService: TreeService
) {

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
}