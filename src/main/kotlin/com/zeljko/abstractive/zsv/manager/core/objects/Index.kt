package com.zeljko.abstractive.zsv.manager.core.objects

data class Index(
    val ctime: Long, // creation/change time, nanoseconds
    val mtime: Long, // modification time, nanoseconds
    val dev: Int, // The ID of device containing this file
    val ino: Long, // the file's inode number
    val mode: Int,
    val uid: Int, // User ID of owner
    val gid: Int, // Group ID of owner
    val sha: String?,
    val flags: Int,
    val path: String
)
