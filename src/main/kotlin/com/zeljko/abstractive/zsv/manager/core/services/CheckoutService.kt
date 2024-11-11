package com.zeljko.abstractive.zsv.manager.core.services

import com.zeljko.abstractive.zsv.manager.utils.FileUtils
import com.zeljko.abstractive.zsv.manager.utils.FileUtils.getCurrentPath
import org.springframework.stereotype.Service
import java.nio.file.Files
import java.nio.file.Path


@Service
class CheckoutService(
    private val commitService: CommitService,
    private val treeService: TreeService
) {
    fun checkout(commitSha: String) {
        val currentPath = getCurrentPath()
        cleanWorkingDirectory(currentPath)

        val (treeSha, _) = commitService.decompress(commitSha, currentPath)
        val decompressedTree = treeService.getDecompressedTreeContent(treeSha, currentPath)
        treeService.extractToDisk(decompressedTree, treeSha, currentPath, currentPath)

        FileUtils.updateCurrentHead(commitSha)
    }

    private fun cleanWorkingDirectory(path: Path) {
        Files.walk(path)
            .filter { !it.startsWith(path.resolve(".git")) }
            .filter { it != path }
            .sorted(Comparator.reverseOrder())
            .forEach { Files.delete(it) }
    }
}