package com.zeljko.abstractive.zsv.manager.blob

import com.zeljko.abstractive.zsv.manager.utils.*
import com.zeljko.abstractive.zsv.manager.utils.FileUtils.getObjectShaPath
import com.zeljko.abstractive.zsv.manager.utils.FileUtils.storeBlob
import org.springframework.stereotype.Service
import java.nio.file.Files
import java.nio.file.Paths


@Service
class BlobService {
    fun decompressBlobObject(blobSha: String): Blob {
        if (blobSha.length != 40) {
            throw InvalidHashException("Invalid blob hash. It must be exactly 40 characters long.")
        }

        val path = getObjectShaPath(blobSha)
        val compressedContent = Files.readAllBytes(path)
        val decompressedContent = compressedContent.zlibDecompress()

        val header = decompressedContent.take(4).toByteArray().toString(Charsets.UTF_8)

        if (header != "blob") {
            throw InvalidObjectHeaderException("Not a blob object")
        }

        return Blob(
            content = decompressedContent,
            blobSha = blobSha
        )
    }

    fun compressFileToBlobObject(write: Boolean, fileToCompress: String): String {
        val path = Paths.get(fileToCompress)
        val fileContent = Files.readString(path)

        val blobHeader = "blob ${fileContent.length}\u0000"
        val content = blobHeader + fileContent
        val compressedContent = content.zlibCompress()

        // create blob name
        val blobNameSHA1 = content.toSha1()

        val blob = Blob(
            content = compressedContent,
            blobSha = blobNameSHA1
        )

        val currentDirectory = Paths.get("").toAbsolutePath()
        storeBlob(currentDirectory, blob.blobSha, blob.content)

        if (write) {
            val objectsDirectory = currentDirectory.resolve(".zsv/objects")
            storeBlob(objectsDirectory, blob.blobSha, blob.content)
        }

        return blob.blobSha
    }
}