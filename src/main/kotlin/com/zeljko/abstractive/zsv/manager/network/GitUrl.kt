package com.zeljko.abstractive.zsv.manager.network

data class GitUrl(
    val host: String,
    val port: Int = 9418, // default git protocol port
    val path: String
)
