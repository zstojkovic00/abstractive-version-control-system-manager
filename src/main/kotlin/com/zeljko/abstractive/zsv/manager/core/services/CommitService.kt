package com.zeljko.abstractive.zsv.manager.core.services

import com.zeljko.abstractive.zsv.manager.core.objects.Commit
import com.zeljko.abstractive.zsv.manager.utils.FileUtils.OBJECTS_DIR
import com.zeljko.abstractive.zsv.manager.utils.FileUtils.getCurrentHead
import com.zeljko.abstractive.zsv.manager.utils.FileUtils.getCurrentPath
import com.zeljko.abstractive.zsv.manager.utils.FileUtils.getObjectShaPath
import com.zeljko.abstractive.zsv.manager.utils.FileUtils.storeObject
import com.zeljko.abstractive.zsv.manager.utils.InvalidHashException
import com.zeljko.abstractive.zsv.manager.utils.toSha1
import com.zeljko.abstractive.zsv.manager.utils.zlibCompress
import com.zeljko.abstractive.zsv.manager.utils.zlibDecompress
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Service
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Service
class CommitService(
    private val treeService: TreeService,
    private val eventPublisher: ApplicationEventPublisher
) {
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
        val sha = commit.toSha1()

        val currentDirectory = getCurrentPath()
        val objectsDirectory = currentDirectory.resolve(OBJECTS_DIR)
        storeObject(objectsDirectory, sha, compressedContent)
        return sha
    }

    fun commit(message: String) {
        val treeSha = treeService.compressFromFile()
        val parentSha = getCurrentHead()
        val sha = compressFromMessage(message, treeSha, parentSha)
        // update branch commit
        eventPublisher.publishEvent(sha)
    }

    fun compressFromContent(decompressedContent: ByteArray, path: Path): Any {
        val commitHeader = "commit ${decompressedContent.size}\u0000".toByteArray()
        val content = commitHeader + decompressedContent

        val compressedContent = content.zlibCompress()
        val sha = content.toSha1()

        val objectsDirectory = path.resolve(OBJECTS_DIR)
        storeObject(objectsDirectory, sha, compressedContent)
        return sha
    }


    fun decompress(sha: String, basePath: Path = getCurrentPath()): Commit {
        if (sha.length != 40) {
            throw InvalidHashException("Invalid hash. It must be exactly 40 characters long.")
        }

        val path = getObjectShaPath(basePath, sha)
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
        val author =
            lines.first { it.startsWith("author") }.substringAfter("author ").substringBeforeLast(">").trim() + ">"
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

    fun readCommitRecursively(sha: String = getCurrentHead()) {
        val commit = decompress(sha)
        prettyPrintCommit(sha, commit)

        if (commit.parentSha != null) {
            readCommitRecursively(commit.parentSha)
        }
    }

    private fun prettyPrintCommit(sha: String, commit: Commit) {
        return println(
            """
            commit  $sha
            Author: ${commit.author}
            Date:   ${LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)}
          
                    ${commit.message}
                   
        """.trimIndent()
        )
    }
}