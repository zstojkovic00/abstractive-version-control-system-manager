package com.zeljko.abstractive.zsv.manager.core.services

import com.zeljko.abstractive.zsv.manager.core.objects.FileChange
import com.zeljko.abstractive.zsv.manager.utils.FileUtils
import com.zeljko.abstractive.zsv.manager.utils.FileUtils.ZSV_DIR
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
    private val treeService: TreeService,
    private val blobService: BlobService
) {

    /**
    1. The target branch is an ancestor of the current branch
    master (currentBranch) A -> B -> C -> D (HEAD)
    feature (target)   A -> B
    No merge is needed

    2. The currentBranch branch is an ancestor of the target branch
    master (currentBranch) A -> B (HEAD)
    feature (target)   A -> B -> C -> D
    In this case we only update HEAD to last commit of target (D)

    3. The currentBranch branch and the target branch are not related.
    master (currentBranch) A -> B -> C (HEAD)
    \
    feature (target)         -> D -> E
     **/
    fun merge(targetBranchName: String, currentBranchName: String = getCurrentBranchName()) {

        if (targetBranchName == currentBranchName) {
            throw IllegalStateException("Already on branch $currentBranchName")
        }

        if (!checkIfBranchExists(targetBranchName)) {
            throw IllegalArgumentException("There is no branch $targetBranchName")
        }

        val currentCommitSha = getCurrentHead()
        val targetCommitSha = getShaFromBranchName(targetBranchName)
        val lca = findLastCommonAncestor(currentCommitSha, targetCommitSha)

        if (lca == targetCommitSha) {
            println("Already up-to-date")
        } else if (lca == currentCommitSha) {
            println("Fast forward")
            fastForwardMerge(targetCommitSha, currentCommitSha, currentBranchName)
        } else {
            // merge
        }
    }


    /**
    Finds the Last Common Ancestor (LCA) between two commits in a git-like commit history.

    master: A -> B -> C -> D (HEAD) depth = 4
    \
    feature:      E -> F depth = 3
     **/
    private fun findLastCommonAncestor(currentCommitSha: String, targetCommitSha: String): String {
        println("Head commit $currentCommitSha")
        println("Target commit $targetCommitSha")

        val targetCommitDepth = commitService.getCommitDepth(targetCommitSha)
        val currentCommitDepth = commitService.getCommitDepth(currentCommitSha)
        println("Head commit depth $currentCommitDepth")
        println("Target commit depth $targetCommitDepth")

        var currentCommit = currentCommitSha
        var targetCommit = targetCommitSha

        if (currentCommitDepth > targetCommitDepth) {
            for (i in 0 until (currentCommitDepth - targetCommitDepth)) {
                currentCommit = commitService.decompress(currentCommit).parentSha!!
            }
        } else if (currentCommitDepth < targetCommitDepth) {
            for (i in 0 until (targetCommitDepth - currentCommitDepth)) {
                targetCommit = commitService.decompress(targetCommit).parentSha!!
            }
        }

        while (currentCommit != targetCommit) {
            currentCommit = commitService.decompress(currentCommit).parentSha!!
            targetCommit = commitService.decompress(targetCommit).parentSha!!
        }

        return currentCommit
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
        if (!checkIfBranchExists(branchName)) {
            throw IllegalArgumentException("There is no branch $branchName")
        }
        // target branch tree
        val commitSha = getShaFromBranchName(branchName)
        val (targetTreeSha, _) = commitService.decompress(commitSha)

        // current branch tree
        val currentCommit = getCurrentHead()
        val (currentTreeSha, _) = commitService.decompress(currentCommit)

        val changes = treeService.findChanges(targetTreeSha, currentTreeSha)
        applyChangesToWorkingDirectory(changes)

        updateHeadReference(branchName)
    }

    private fun fastForwardMerge(targetCommitSha: String, currentCommitSha: String, currentBranchName: String) {
        val (targetTreeSha, _) = commitService.decompress(targetCommitSha)
        val (currentTreeSha, _) = commitService.decompress(currentCommitSha)

        val changes = treeService.findChanges(targetTreeSha, currentTreeSha)
        applyChangesToWorkingDirectory(changes)

        updateHeadReference(currentBranchName)
    }

    private fun applyChangesToWorkingDirectory(changes: MutableMap<String, MutableList<FileChange>>) {
        changes.forEach { (action, fileChange) ->
            when (action) {
                "ADDED" -> {
                    fileChange.forEach { file ->
                        val content = blobService.decompress(file.tree.sha)
                        val targetPath = getCurrentPath().resolve(file.fullPath)
                        Files.createDirectories(targetPath.parent)
                        Files.write(targetPath, content.getContentWithoutHeader())
                    }
                }

                "MODIFIED" -> {
                    fileChange.forEach { file ->
                        val content = blobService.decompress(file.tree.sha)
                        val targetPath = getCurrentPath().resolve(file.fullPath)
                        Files.write(targetPath, content.getContentWithoutHeader())
                    }
                }

                "DELETED" -> {
                    fileChange.forEach { file ->
                        val target = getCurrentPath().resolve(file.fullPath)
                        Files.deleteIfExists(target)
                    }
                }
            }
        }
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
            branchName.substringAfter("ref: refs/heads/").trim()
        } else {
            "HEAD" // detached HEAD state
        }
    }

    fun createNewBranch(branchName: String, commitSha: String) {
        val branchPath = Paths.get("${FileUtils.HEADS_DIR}/$branchName")
        Files.createDirectories(branchPath.parent)
        Files.writeString(branchPath, "$commitSha\n")
    }

    fun getShaFromBranchName(branchName: String): String {
        val branchPath = Paths.get("${FileUtils.HEADS_DIR}/$branchName")
        return Files.readString(branchPath).trim()
    }

    fun checkIfBranchExists(branchName: String): Boolean {
        return Files.exists(Paths.get("${FileUtils.HEADS_DIR}/$branchName"))
    }

}
