package com.zeljko.abstractive.zsv.manager.core.objects

data class Index(
    val ctime: Long,
    val mtime: Long,
    val dev: Int,
    val ino: Long,
    val objectType: ObjectType,
    val uid: Int,
    val gid: Int,
    val sha: String,
    val flags: Int,
    val path: String
)
