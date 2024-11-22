package com.zeljko.abstractive.zsv.manager.core.services

import com.zeljko.abstractive.zsv.manager.core.objects.IndexEntry
import com.zeljko.abstractive.zsv.manager.core.objects.ObjectType
import com.zeljko.abstractive.zsv.manager.utils.FileUtils.INDEX_DIR
import com.zeljko.abstractive.zsv.manager.utils.toShaByte
import org.springframework.stereotype.Service
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.attribute.BasicFileAttributes


@Service
class IndexService(
    val blobService: BlobService
) {
    val indexPath = Paths.get(INDEX_DIR)

    enum class FileStatus {
        MODIFY, ADD, REMOVE, CONFLICT, SAME
    }

    fun saveFileToIndex(filePath: Path) {

        if (!Files.exists(indexPath)) {
            Files.createFile(indexPath)
            val buffer: ByteBuffer = ByteBuffer.allocate(12)
            buffer.position(0).put("DIRC".toByteArray())
            buffer.position(4).putInt(2)
            buffer.position(8).putInt(0)
            Files.write(indexPath, buffer.array())
        }

        val indexEntry = getFileAttributes(filePath)

//        if (isFileInIndex()) {
//
//        }

        writeIndexEntry(filePath, indexEntry)
    }

    private fun writeIndexEntry(filePath: Path, indexEntry: IndexEntry) {
        RandomAccessFile(indexPath.toFile(), "rw").use { file ->
            file.seek(8)
            val entries = file.readInt()

            file.seek(8)
            file.writeInt(entries + 1)

            file.seek(file.length())

            val blobSha = blobService.compressFromFile(true, filePath)

            file.writeLong(indexEntry.ctime)
            file.writeLong(indexEntry.mtime)
            file.writeLong(indexEntry.dev)
            file.writeLong(indexEntry.ino)
            file.writeInt(indexEntry.mode)
            file.writeInt(indexEntry.uid)
            file.writeInt(indexEntry.gid)
            file.write(blobSha.toShaByte())
            file.writeInt(indexEntry.flags)
            file.writeBytes(indexEntry.pathName)
            file.write(0)
        }
    }

    private fun isFileInIndex(): Boolean {
        TODO("Not yet implemented")
    }


    fun getFileAttributes(path: Path): IndexEntry {
        val attributes = Files.readAttributes(path, BasicFileAttributes::class.java)

        val objectType: ObjectType = when {
            Files.isSymbolicLink(path) -> ObjectType.SYMBOLIC_LINK
            Files.isExecutable(path) -> ObjectType.EXECUTABLE_FILE
            else -> ObjectType.REGULAR_FILE
        }

        return IndexEntry(
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

    fun parseIndexFile(): List<IndexEntry> {
        val indexEntries = mutableListOf<IndexEntry>()
        val bytes = indexPath.toFile().readBytes()
        val buffer = ByteBuffer.wrap(bytes)

        val header = ByteArray(4)
        buffer.get(header)
        if (String(header) != "DIRC") {
            throw IllegalStateException("This is not index file, missing DIRC signature")
        }

        val version = buffer.getInt()
        val entries = buffer.getInt()
        println("Version of index file: $version")
        println("Number of entries: $entries")

        for (entry in 1..entries) {
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
            indexEntries.add(
                IndexEntry(
                    ctime, mtime, dev, ino,
                    mode, uid, gid, sha.toString(StandardCharsets.UTF_8),
                    flags, pathName
                )
            )
        }
        return indexEntries
    }


}