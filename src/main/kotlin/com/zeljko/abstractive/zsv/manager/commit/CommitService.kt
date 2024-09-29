package com.zeljko.abstractive.zsv.manager.commit

import com.zeljko.abstractive.zsv.manager.tree.TreeService
import com.zeljko.abstractive.zsv.manager.utils.FileUtils.getCurrentHead
import com.zeljko.abstractive.zsv.manager.utils.FileUtils.storeObject
import com.zeljko.abstractive.zsv.manager.utils.FileUtils.updateCurrentHead
import com.zeljko.abstractive.zsv.manager.utils.toSha1
import com.zeljko.abstractive.zsv.manager.utils.zlibCompress
import org.springframework.stereotype.Service
import java.nio.file.Paths

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

    /**
     * Creates a new commit object.
     *
     * @param message The commit message
     * @param treeSha The SHA of the tree object this commit points to
     * @param parentSha The SHA of the parent commit (optional)
     * @return The SHA of the created commit
     */
    fun commitTree(message: String, treeSha: String, parentSha: String): String {
        val commitBuilder = StringBuilder()

        commitBuilder.append("tree $treeSha\n")

        if (parentSha.isNotEmpty()) {
            commitBuilder.append("parent $parentSha\n")
        }

        // TODO: Replace with dynamic timestamp and user info from configuration
        val author = "author zstojkovicTEST00 <00zeljkostojkovic@gmail.com> 1727635374 +0200\n"
        val committer = "committer zstojkovicTEST00 <00zeljkostojkovic@gmail.com> 1727635374 +0200\n"

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

        val currentDirectory = Paths.get("").toAbsolutePath()
        val objectsDirectory = currentDirectory.resolve(".git/objects")
        storeObject(objectsDirectory, commitSha, compressedContent)
        return commitSha
    }

    fun commit(message: String): String {

        val treeSha = treeService.compressTreeObject(Paths.get("").toAbsolutePath())
        val parentSha = getCurrentHead()
        val commitSha = commitTree(message, treeSha, parentSha)

        // update HEAD
        updateCurrentHead(commitSha)

        return commitSha
    }
}