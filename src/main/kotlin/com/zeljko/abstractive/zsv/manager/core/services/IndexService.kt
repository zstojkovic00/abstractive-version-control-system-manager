package com.zeljko.abstractive.zsv.manager.core.services

import com.zeljko.abstractive.zsv.manager.core.objects.Index
import com.zeljko.abstractive.zsv.manager.core.objects.ObjectType
import com.zeljko.abstractive.zsv.manager.utils.FileUtils.INDEX_DIR
import com.zeljko.abstractive.zsv.manager.utils.toShaByte
import org.springframework.stereotype.Service
import java.io.DataOutputStream
import java.io.FileOutputStream
import java.io.RandomAccessFile
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.attribute.BasicFileAttributes


@Service
class IndexService(
    val blobService: BlobService
) {
    fun saveFileToIndex(path: Path) {
        val indexPath = path.resolve(INDEX_DIR)

        if (!Files.exists(indexPath)) {
            Files.createDirectory(indexPath.parent)
            DataOutputStream(FileOutputStream(indexPath.toFile())).use { output ->
                output.writeBytes("DIRC")
                output.writeInt(2)
                output.writeInt(0)
            }
        }

        RandomAccessFile(indexPath.toFile(), "rw").use { file ->
            file.seek(8)
            val entries = file.readInt()

            file.seek(8)
            file.writeInt(entries + 1)

            file.seek(file.length())

            val blobSha = blobService.compressFromFile(true, path)
            val (ctime, mtime, dev,
                ino, mode, uid,
                gid, _, flags, pathName) = getFileAttributes(path)

            file.writeLong(ctime)
            file.writeLong(mtime)
            file.writeInt(dev)
            file.writeLong(ino)
            file.writeInt(mode)
            file.writeInt(uid)
            file.writeInt(gid)
            file.write(blobSha.toShaByte())
            file.writeInt(flags)
            file.writeBytes(pathName)
            file.write(0)
        }
    }


    fun getFileAttributes(path: Path): Index {
        val attributes = Files.readAttributes(path, BasicFileAttributes::class.java)

        val objectType: ObjectType = when {
            Files.isSymbolicLink(path) -> ObjectType.SYMBOLIC_LINK
            Files.isExecutable(path) -> ObjectType.EXECUTABLE_FILE
            else -> ObjectType.REGULAR_FILE
        }

        return Index(
            ctime = attributes.creationTime().toMillis(),
            mtime = attributes.lastModifiedTime().toMillis(),
            dev = Files.getAttribute(path, "unix:dev") as Int,
            ino = Files.getAttribute(path, "unix:ino") as Long,
            mode = objectType.mode.toInt(),
            uid = Files.getAttribute(path, "unix:uid") as Int,
            gid = Files.getAttribute(path, "unix:gid") as Int,
            sha = null,
            flags = 0,
            path = path.toString()
        )
    }

    fun parseIndexFile() {
        // TODO: implement this
    }


}