package com.zeljko.abstractive.zsv.manager.commit

import com.zeljko.abstractive.zsv.manager.utils.FileUtils.storeObject
import com.zeljko.abstractive.zsv.manager.utils.toSha1
import com.zeljko.abstractive.zsv.manager.utils.zlibCompress
import org.springframework.stereotype.Service
import java.nio.file.Paths
import java.time.Instant
import java.time.ZoneOffset


@Service
class CommitService {

    /* commit {size}\0{content}

        {content}:
        tree {tree sha} // Sha of the tree object this commit points to
        {parents} // Optional list of parent commit objects
        author {author_name} <{author_email}> {author_Date_seconds} {author_date_timezone}
        committer {committer_name} <{committer_email}> {committer_date_seconds}

        {commit message}
        */

    fun commitTree(message: String, treeSha: String, parentSha: String) : String {

        val commitBuilder = StringBuilder()

        commitBuilder.append("tree $treeSha\n")

        if (parentSha != null) {
            commitBuilder.append("parent $parentSha\n")
        }

        val timestamp = Instant.now().atOffset(ZoneOffset.UTC)
        val author = "author Zeljko Stojkovic <00zeljkostojkovic@gmail.com> ${timestamp.toEpochSecond()} + 0000\n"
        val committer = "committer Zeljko Stojkovic <00zeljkostojkovic@gmail.com> ${timestamp.toEpochSecond()} + 0000\n"

        commitBuilder.append(author)
        commitBuilder.append(committer)

        commitBuilder.append("")
        commitBuilder.append(message)

        val commitContent = commitBuilder.toString()

        println(commitContent)

        // commit content + header
        val commit = "commit ${commitContent.length}\u0000$commitContent"

        val compressedContent = commit.toByteArray(Charsets.UTF_8).zlibCompress()
        val commitSha = commit.toSha1()

        val currentDirectory = Paths.get("").toAbsolutePath()
        val objectsDirectory = currentDirectory.resolve(".zsv/objects")

        storeObject(objectsDirectory, commitSha, compressedContent)

        return commitSha
    }

}
