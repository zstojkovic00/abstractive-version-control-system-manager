package com.zeljko.abstractive.zsv.manager.core.objects

/**
 * Commit object structure:
 *
 * commit {size}\u0000{content}
 *
 * Content format:
 * tree {tree_sha}
 * parent {parent_sha} (optional, can be multiple)
 * author {author_name} <{author_email}> {author_timestamp} {author_timezone}
 * committer {committer_name} <{committer_email}> {committer_timestamp} {committer_timezone}
 * \n
 * {commit_message}
 **/
data class Commit(
    val treeSha: String,
    val parentSha: String?,
    val author: String,
    val committer: String,
    val message: String
)
