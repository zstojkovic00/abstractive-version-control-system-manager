package com.zeljko.abstractive.zsv.manager.core.objects

import com.zeljko.abstractive.zsv.manager.utils.toHexString
import com.zeljko.abstractive.zsv.manager.utils.toShaByte
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.attribute.BasicFileAttributes

/**
 * Represents an entry in the Git index file (.zsv/index).
 * The index is stored in a custom binary format:
 * - First 12 bytes are header (DIRC + version + number of entries)
 * - Followed by index entries
 * - Each entry is 68 bytes plus variable length path name
 * - Last 20 bytes are SHA-1 of the index
 *
 * Index file format:
 * DIRC <version> <entries>
 * <ctime> <mtime> <dev> <ino> <mode> <uid> <gid> <SHA> <flags> <path>
 * <ctime> <mtime> <dev> <ino> <mode> <uid> <gid> <SHA> <flags> <path>
 * ...
 */
data class IndexEntry(
    val offset: Int?,      // Byte offset in index (not stored)
    val ctime: Long,       // Creation time (8 bytes)
    val mtime: Long,       // Modified time (8 bytes)
    val dev: Long,         // Device ID (8 bytes)
    val ino: Long,         // Inode number (8 bytes)
    val mode: Int,         // Mode (4 bytes)
    val uid: Int,          // User ID (4 bytes)
    val gid: Int,          // Group ID (4 bytes)
    val sha: String?,      // SHA-1 hash (20 bytes)
    val flags: Int,        // Flags (4 bytes)
    val pathName: String   // Path + null terminator
) {
    data class FileStatus(
        val mtime: Long,
        val ino: Long,
        val pathName: String
    )

    fun serialize(file: RandomAccessFile, blobSha: String) {
        file.writeLong(ctime)
        file.writeLong(mtime)
        file.writeLong(dev)
        file.writeLong(ino)
        file.writeInt(mode)
        file.writeInt(uid)
        file.writeInt(gid)
        file.write(blobSha.toShaByte())
        file.writeInt(flags)
        file.writeBytes(pathName)
        file.write(0)
    }

    companion object {
        fun deserialize(buffer: ByteBuffer): IndexEntry {
            val offset = buffer.position()
            val ctime = buffer.getLong()
            val mtime = buffer.getLong()
            val dev = buffer.getLong()
            val ino = buffer.getLong()
            val mode = buffer.getInt()
            val uid = buffer.getInt()
            val gid = buffer.getInt()

            val sha = ByteArray(20)
            buffer.get(sha)

            val flags = buffer.getInt()

            val pathBuilder = StringBuilder()
            var byte = buffer.get()
            while (byte != 0.toByte()) {
                pathBuilder.append(byte.toInt().toChar())
                byte = buffer.get()
            }
            val pathName = pathBuilder.toString()

            return IndexEntry(
                offset, ctime, mtime, dev, ino,
                mode, uid, gid, sha.toHexString(),
                flags, pathName
            )
        }

        fun getFileAttributes(path: Path): IndexEntry {
            val attributes = Files.readAttributes(path, BasicFileAttributes::class.java)

            val objectType: ObjectType = when {
                Files.isSymbolicLink(path) -> ObjectType.SYMBOLIC_LINK
                Files.isExecutable(path) -> ObjectType.EXECUTABLE_FILE
                else -> ObjectType.REGULAR_FILE
            }

            return IndexEntry(
                offset = null,
                ctime = attributes.creationTime().toMillis(),
                mtime = attributes.lastModifiedTime().toMillis(),
                dev = Files.getAttribute(path, "unix:dev") as Long,
                ino = Files.getAttribute(path, "unix:ino") as Long,
                mode = objectType.mode.toInt(),
                uid = Files.getAttribute(path, "unix:uid") as Int,
                gid = Files.getAttribute(path, "unix:gid") as Int,
                sha = null,
                flags = 0,
                pathName = path.toString()
            )
        }
    }
}