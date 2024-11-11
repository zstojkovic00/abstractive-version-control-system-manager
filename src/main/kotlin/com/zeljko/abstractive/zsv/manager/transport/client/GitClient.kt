package com.zeljko.abstractive.zsv.manager.transport.client

interface GitClient {
    fun clone(url: String)
}