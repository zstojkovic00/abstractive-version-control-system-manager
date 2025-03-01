package com.zeljko.abstractive.zsv.manager.transport

interface RemoteRepositoryClient {
    fun push(branchName: String): String
    fun pull(branchName: String): String
}