package com.zeljko.abstractive.zsv.manager.core.objects


/*

The index, which is a single file at .zsv/index, is stored in a custom binary format
The first 12 bytes are the header, the last 20 a SHA-1 hash of the index,
and the bytes in between are index entries, each 62 bytes plus the length of the path

The index is a binary file that has the following format:

    DIRC <version_number> <number of entries> // 12 bytes

    <ctime> <mtime> <dev> <ino> <mode> <uid> <gid> <SHA> <flags> <path> //
    <ctime> <mtime> <dev> <ino> <mode> <uid> <gid> <SHA> <flags> <path>
    <ctime> <mtime> <dev> <ino> <mode> <uid> <gid> <SHA> <flags> <path>

    # more entries

 */
// 68 bytes without header and path name
data class IndexEntry(
    val ctime: Long, // creation/change time, nanoseconds // 8
    val mtime: Long, // modification time, nanoseconds // 8
    val dev: Long, // The ID of device containing this file // 8
    val ino: Long, // the file's inode number // 8
    val mode: Int, // 4
    val uid: Int, // User ID of owner // 4
    val gid: Int, // Group ID of owner // 4
    val sha: String?, // 20
    val flags: Int, // 4
    val pathName: String // test.txt 8 + nul = 9
)
