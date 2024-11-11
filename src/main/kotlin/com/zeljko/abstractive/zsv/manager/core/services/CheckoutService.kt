package com.zeljko.abstractive.zsv.manager.core.services

import com.zeljko.abstractive.zsv.manager.utils.FileUtils
import com.zeljko.abstractive.zsv.manager.utils.FileUtils.getCurrentPath
import org.springframework.stereotype.Service
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths


@Service
class CheckoutService(
    private val commitService: CommitService,
    private val treeService: TreeService
) {
    fun checkout(branchName: String? = null, newBranch: String? = null) {
        val currentPath = getCurrentPath()

        if (newBranch != null) {
            val newBranchPath = checkIfBranchExist(newBranch)

            val headContent = FileUtils.getCurrentHead()
            val currentCommit = if (headContent.startsWith("ref: ")) {
                val refPath = headContent.removePrefix("ref: ")
                Files.readString(Paths.get(".git", refPath)).trim()
            } else {
                headContent
            }

            // create new branch which points to current commit
            Files.createDirectories(newBranchPath.parent)
            Files.writeString(newBranchPath, currentCommit)

            FileUtils.updateCurrentHead("ref: refs/heads/$newBranch")
            return
        }

        val (commitSha, isBranch) = resolveBranch(branchName!!)

        cleanWorkingDirectory(currentPath)

        val (treeSha, _) = commitService.decompress(commitSha, currentPath)
        val decompressedTree = treeService.getDecompressedTreeContent(treeSha, currentPath)
        treeService.extractToDisk(decompressedTree, treeSha, currentPath, currentPath)

        if (isBranch) {
            FileUtils.updateCurrentHead("ref: refs/heads/$branchName")
        } else {
            FileUtils.updateCurrentHead(commitSha)
        }
    }

    private fun checkIfBranchExist(newBranch: String): Path {
        val newBranchPath = Paths.get(".git/refs/heads/$newBranch")
        if (Files.exists(newBranchPath)) {
            throw IllegalArgumentException("Branch $newBranch already exists")
        }
        return newBranchPath
    }

    private fun resolveBranch(branch: String): Pair<String, Boolean> {
        if (branch.length == 40) {
            return Pair(branch, false)
        }

        val branchPath = Paths.get(".git/refs/heads/$branch")
        if (Files.exists(branchPath)) {
            val commitSha = Files.readString(branchPath).trim()
            return Pair(commitSha, true)
        }
        throw IllegalArgumentException("Invalid branch $branch name")
    }

    private fun cleanWorkingDirectory(path: Path) {
        Files.walk(path)
            .filter { !it.startsWith(path.resolve(".git")) }
            .filter { it != path }
            .sorted(Comparator.reverseOrder())
            .forEach { Files.delete(it) }
    }
}