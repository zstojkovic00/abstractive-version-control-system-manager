package com.zeljko.abstractive.zsv.manager.core.objects

data class Commit(
    val treeSha: String,
    val parentSha: String?,
    val author: String,
    val committer: String,
    val message: String
)
