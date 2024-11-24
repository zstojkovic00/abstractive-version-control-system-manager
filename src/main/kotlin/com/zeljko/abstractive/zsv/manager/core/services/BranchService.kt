package com.zeljko.abstractive.zsv.manager.core.services

import com.zeljko.abstractive.zsv.manager.utils.FileUtils
import com.zeljko.abstractive.zsv.manager.utils.FileUtils.ZSV_DIR
import com.zeljko.abstractive.zsv.manager.utils.FileUtils.cleanWorkingDirectory
import com.zeljko.abstractive.zsv.manager.utils.FileUtils.getCurrentHead
import com.zeljko.abstractive.zsv.manager.utils.FileUtils.getCurrentPath
import com.zeljko.abstractive.zsv.manager.utils.FileUtils.updateHeadReference
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Service
import java.nio.file.Files
import java.nio.file.Paths


@Service
class BranchService(
    private val commitService: CommitService,
    private val treeService: TreeService
) {

    /**
    1. The giver branch is an ancestor of the current branch
    master (receiver) A -> B -> C -> D (HEAD)
    feature (giver)   A -> B
    No merge is needed

    2. The receiver branch is an ancestor of the giver branch
    master (receiver) A -> B (HEAD)
    feature (giver)   A -> B -> C -> D
    In this case we only update HEAD to last commit of giver (D)

    3. The receiver branch and the giver branch are not related.
    master (receiver) A -> B -> C (HEAD)
                           \
    feature (giver)         -> D -> E
     **/
    fun merge(giver: String, receiver: String = getCurrentBranchName()) {

        if (giver == receiver) {
            throw IllegalStateException("Already on branch $receiver")
        }

//        if (checkIfBranchExists(giver)) {
//            throw IllegalArgumentException("There is no branch $giver")
//        }

        lastCommonAncestor(giver, receiver)
    }


    /**
    master: A -> B -> C -> D (HEAD) depth = 4
                 \
    feature:     E -> F depth = 3
     **/
    private fun lastCommonAncestor(giver: String, receiver: String) {
        val headCommit = getCurrentHead()
        val giverCommit = readCommitShaFromBranchName(giver)
        println("Head commit $headCommit")
        println("Giver commit")

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
