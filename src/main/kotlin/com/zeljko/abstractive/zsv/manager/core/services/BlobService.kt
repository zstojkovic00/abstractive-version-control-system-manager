package com.zeljko.abstractive.zsv.manager.core.services

import com.zeljko.abstractive.zsv.manager.core.objects.Blob
import com.zeljko.abstractive.zsv.manager.utils.*
import com.zeljko.abstractive.zsv.manager.utils.FileUtils.OBJECTS_DIR
import com.zeljko.abstractive.zsv.manager.utils.FileUtils.getCurrentPath
import com.zeljko.abstractive.zsv.manager.utils.FileUtils.getObjectShaPath
import com.zeljko.abstractive.zsv.manager.utils.FileUtils.storeObject
import org.springframework.stereotype.Service
import java.nio.file.Files
import java.nio.file.Path


@Service
class BlobService {
    fun decompress(sha: String, basePath: Path): Blob {
        if (sha.length != 40) {
            throw InvalidHashException("Invalid blob hash. It must be exactly 40 characters long.")
        }

        val path = getObjectShaPath(basePath, sha)
        val compressedContent = Files.readAllBytes(path)
        val decompressedContent = compressedContent.zlibDecompress()

        val header = decompressedContent.take(4).toByteArray().toString(Charsets.UTF_8)

        if (header != "blob") {
            throw InvalidObjectHeaderException("Not a blob object")
        }

        return Blob(
            content = decompressedContent,
            sha = sha
        )
    }

    fun compressFromFile(write: Boolean, path: Path): String {
        val fileContent = Files.readAllBytes(path)

        val blobHeader = "blob ${fileContent.size}\u0000".toByteArray(Charsets.UTF_8)
        val content = blobHeader + fileContent
        val compressedContent = content.zlibCompress()

        // create blob name
        val blobSHA1 = content.toSha1()

        val blob = Blob(
            content = compressedContent,
            sha = blobSHA1
        )

        val currentDirectory = getCurrentPath()
//        storeObject(currentDirectory, blob.sha, blob.content)

        if (write) {
            val objectsDirectory = currentDirectory.resolve(OBJECTS_DIR)
            storeObject(objectsDirectory, blob.sha, blob.content)
        }

        return blob.sha
    }


    fun compressFromContent(content: ByteArray, path: Path): String {

        val blobHeader = "blob ${content.size}\u0000".toByteArray(Charsets.UTF_8)
        val fullContent = blobHeader + content
        val compressedContent = fullContent.zlibCompress()

        // create blob name
        val blobSHA1 = fullContent.toSha1()

        val blob = Blob(
            content = compressedContent,
            sha = blobSHA1
        )

        val objectsDirectory = path.resolve(OBJECTS_DIR)
        storeObject(objectsDirectory, blob.sha, blob.content)

        return blob.sha
    }
}