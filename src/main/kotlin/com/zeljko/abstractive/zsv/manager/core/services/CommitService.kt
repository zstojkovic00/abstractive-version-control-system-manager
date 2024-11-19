package com.zeljko.abstractive.zsv.manager.core.services

import com.zeljko.abstractive.zsv.manager.core.objects.Commit
import com.zeljko.abstractive.zsv.manager.utils.FileUtils.OBJECTS_DIR
import com.zeljko.abstractive.zsv.manager.utils.FileUtils.getCurrentHead
import com.zeljko.abstractive.zsv.manager.utils.FileUtils.getCurrentPath
import com.zeljko.abstractive.zsv.manager.utils.FileUtils.getObjectShaPath
import com.zeljko.abstractive.zsv.manager.utils.FileUtils.storeObject
import com.zeljko.abstractive.zsv.manager.utils.FileUtils.updateBranchCommit
import com.zeljko.abstractive.zsv.manager.utils.InvalidHashException
import com.zeljko.abstractive.zsv.manager.utils.toSha1
import com.zeljko.abstractive.zsv.manager.utils.zlibCompress
import com.zeljko.abstractive.zsv.manager.utils.zlibDecompress
import org.springframework.stereotype.Service
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Service
class CommitService(
    private val treeService: TreeService
) {

    /**
     * Commit object structure:
     * commit {size}\0{content}
     *
     * Content format:
     * tree {tree_sha}
     * parent {parent_sha} (optional, can be multiple)
     * author {author_name} <{author_email}> {author_timestamp} {author_timezone}
     * committer {committer_name} <{committer_email}> {committer_timestamp} {committer_timezone}
     *
     * {commit_message}
     * -------------------------------------------------------------------------------------------
     * Usage example:
     * 1. Create tree: git write-tree -> 9acc3277ad88cd6e2d489a81cf82bf3b3dc5d012
     * 2. Get current HEAD: git rev-parse HEAD -> 7f8812a9e2e9b36cd2136f7c2e79405ff98e3c28
     * 3. Create commit: zsv commit-tree -m "Test" -t 9acc3277ad88cd6e2d489a81cf82bf3b3dc5d012 -p 7f8812a9e2e9b36cd2136f7c2e79405ff98e3c28
     *    Output: b4ffac0fc29939a6ba99fe5a15fc5cfb270ac4cb
     * 4. Update HEAD: git reset --hard b4ffac0fc29939a6ba99fe5a15fc5cfb270ac4cb
     * 5. Verify: git log
     */
    fun compressFromMessage(message: String, treeSha: String, parentSha: String): String {
        val commitBuilder = StringBuilder()

        commitBuilder.append("tree $treeSha\n")

        if (parentSha.isNotEmpty()) {
            commitBuilder.append("parent $parentSha\n")
        }

        // TODO: Replace with dynamic timestamp and user info from configuration
        val author = "author zeljko <00zeljkostojkovic@gmail.com> 1727635374 +0200\n"
        val committer = "committer zeljko <00zeljkostojkovic@gmail.com> 1727635374 +0200\n"

        commitBuilder.append(author)
        commitBuilder.append(committer)
        commitBuilder.append("\n")
        commitBuilder.append(message)

        val commitContent = commitBuilder.toString()
        println(commitContent)

        // commit object (header + content)
        val commit = "commit ${commitContent.length}\u0000$commitContent"

        val compressedContent = commit.toByteArray(Charsets.UTF_8).zlibCompress()
        val commitSha = commit.toSha1()

        val currentDirectory = getCurrentPath()
        val objectsDirectory = currentDirectory.resolve(OBJECTS_DIR)
        storeObject(objectsDirectory, commitSha, compressedContent)
        return commitSha
    }

    fun commit(message: String): String {
        val treeSha = treeService.compressFromFile(getCurrentPath())
        val parentSha = getCurrentHead()
        val commitSha = compressFromMessage(message, treeSha, parentSha)

        // update HEAD
        updateBranchCommit(commitSha)

        return commitSha
    }

    fun compressFromContent(decompressedContent: ByteArray, path: Path): Any {
        val commitHeader = "commit ${decompressedContent.size}\u0000".toByteArray()
        val content = commitHeader + decompressedContent

        val compressedContent = content.zlibCompress()
        val commitSha = content.toSha1()

        val objectsDirectory = path.resolve(OBJECTS_DIR)
        storeObject(objectsDirectory, commitSha, compressedContent)
        return commitSha
    }


    fun decompress(commitSha: String, basePath: Path = getCurrentPath()): Commit {
        if (commitSha.length != 40) {
            throw InvalidHashException("Invalid hash. It must be exactly 40 characters long.")
        }

        val path = getObjectShaPath(basePath, commitSha)
        val compressedContent = Files.readAllBytes(path)
        val decompressedContent = compressedContent.zlibDecompress()

        // remove header
        return parseCommitFromContent(
            decompressedContent
                .toString(StandardCharsets.UTF_8)
                .substringAfter("\u0000")
        )
    }

    fun parseCommitFromContent(content: String): Commit {
        var lines = content.split("\n")
        val treeSha = lines.first { it.startsWith("tree") }.substringAfter("tree ").trim()
        val parentSha = lines.firstOrNull { it.startsWith("parent") }?.substringAfter("parent ")?.trim()
        val author = lines.first { it.startsWith("author") }.substringAfter("author ").substringBeforeLast(">").trim() + ">"
        val committer = lines.first { it.startsWith("committer") }.substringAfter("committer ").trim()

        val index = lines.indexOf("") + 1
        val message = if (index < lines.size) {
            lines.subList(index, lines.size).joinToString("\n")
        } else {
            ""
        }

        return Commit(
            treeSha = treeSha,
            parentSha = parentSha,
            author = author,
            committer = committer,
            message = message
        )
    }

    fun readCommitRecursively(commitSha: String = getCurrentHead()) {
        val commit = decompress(commitSha)
        prettyPrintCommit(commitSha, commit)

        if (commit.parentSha != null) {
            readCommitRecursively(commit.parentSha)
        }
    }

    private fun prettyPrintCommit(commitSha: String, commit: Commit) {
        return println(
            """
            commit  $commitSha
            Author: ${commit.author}
            Date:   ${LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)}
          
                    ${commit.message}
                   
        """.trimIndent()
        )
    }
}