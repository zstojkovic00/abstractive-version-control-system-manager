package com.zeljko.abstractive.zsv.manager.transport.model

data class GitUrl(
    val host: String,
    val port: Int = 9418, // default git protocol port
    val path: String
)
