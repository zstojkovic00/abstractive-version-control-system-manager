package com.zeljko.abstractive.zsv.manager.core.objects


/*

The index, which is a single file at .zsv/index, is stored in a custom binary format
The first 12 bytes are the header, the last 20 a SHA-1 hash of the index,
and the bytes in between are index entries, each 62 bytes plus the length of the path

The index is a binary file that has the following format:

    DIRC <version_number> <number of entries> // 12 bytes

    <ctime> <mtime> <dev> <ino> <mode> <uid> <gid> <SHA> <flags> <path> // 62 + path.length
    <ctime> <mtime> <dev> <ino> <mode> <uid> <gid> <SHA> <flags> <path>
    <ctime> <mtime> <dev> <ino> <mode> <uid> <gid> <SHA> <flags> <path>

    # more entries

 */
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
