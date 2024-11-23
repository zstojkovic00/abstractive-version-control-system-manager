package com.zeljko.abstractive.zsv.manager.core.services

import com.zeljko.abstractive.zsv.manager.core.objects.IndexEntry
import com.zeljko.abstractive.zsv.manager.utils.FileUtils.getAllFilesWithAttributes
import org.springframework.stereotype.Service

@Service
class RepositoryService(
    private val indexService: IndexService,
    private val branchService: BranchService
) {
    fun status(): String {
        val branchName = branchService.getCurrentBranchName()
        val indexFiles = indexService.getIndexFiles()
        val files = getAllFilesWithAttributes()
        val untrackedFiles = mutableListOf<IndexEntry.FileStatus>()
        val modificationFiles = mutableListOf<IndexEntry.FileStatus>()

        // [1, 4, 5] // sorted
        for (i in files) {
            var b = false
            // [1, 2, 3, 4, 5] // sorted
            for (j in indexFiles) {
                when {
                    i.pathName == j.pathName -> {
                        b = true
                        if (i.mtime != j.mtime || i.ino != j.ino) {
                            modificationFiles.add(i)
                        }
                        break
                    }

                    j.pathName > i.pathName -> {
                        untrackedFiles.add(i)
                        b = true
                        break
                    }
                }

            }
            if (!b) {
                untrackedFiles.add(i)
            }
        }

        return printStatus(branchName, untrackedFiles, modificationFiles)
    }

    private fun printStatus(
        branchName: String,
        untrackedFiles: List<IndexEntry.FileStatus>,
        modificationFiles: List<IndexEntry.FileStatus>
    ): String {
        val statusBuilder = StringBuilder()

        statusBuilder.append("On branch $branchName\n\n")

        if (modificationFiles.isEmpty() && untrackedFiles.isEmpty()) {
            statusBuilder.append("nothing to commit, working tree clean")
            return statusBuilder.toString()
        }

        if (modificationFiles.isNotEmpty()) {
            statusBuilder.append("Changes not staged for commit:\n")
            statusBuilder.append("  (use \"zsv add/rm <file>...\" to update what will be committed)\n")

            modificationFiles.forEach { file ->
                statusBuilder.append("\tmodified:   ${file.pathName}\n")
            }

            statusBuilder.append("\n")
        }

        if (untrackedFiles.isNotEmpty()) {
            statusBuilder.append("Untracked files:\n")
            statusBuilder.append("  (use \"zsv add <file>...\" to include in what will be committed)\n\n")

            untrackedFiles.forEach { file ->
                statusBuilder.append("\t${file.pathName}\n")
            }
            statusBuilder.append("\n")
        }

        return statusBuilder.toString()
    }
}