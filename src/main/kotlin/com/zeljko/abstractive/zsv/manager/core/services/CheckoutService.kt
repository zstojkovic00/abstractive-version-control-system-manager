package com.zeljko.abstractive.zsv.manager.core.services

import com.zeljko.abstractive.zsv.manager.utils.FileUtils.getCurrentPath
import org.springframework.stereotype.Service
import java.nio.file.Files


@Service
class CheckoutService(
    private val commitService: CommitService,
    private val treeService: TreeService
) {
    fun checkout(commitSha: String) {
        val currentPath = getCurrentPath()
        val testFolder = currentPath.resolve("test-folder")
        Files.createDirectories(testFolder)

        val (treeSha, _) = commitService.decompress(commitSha, testFolder)
        val decompressedTree = treeService.getDecompressedTreeContent(treeSha, testFolder)
        treeService.extractToDisk(decompressedTree, treeSha, testFolder, testFolder)
    }
}