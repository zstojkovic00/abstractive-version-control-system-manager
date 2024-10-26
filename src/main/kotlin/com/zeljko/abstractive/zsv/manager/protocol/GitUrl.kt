package com.zeljko.abstractive.zsv.manager.protocol

data class GitUrl(
    val host: String,
    val port: Int = 9418, // default git protocol port
    val path: String
)
