package com.zeljko.abstractive.zsv.manager.transport.model

data class GitReference(
    val sha: String,
    val name: String,
    val capabilities: List<String>? = null
)