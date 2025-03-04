package com.zeljko.abstractive.zsv.manager.command

import com.zeljko.abstractive.zsv.manager.core.services.BlobService
import com.zeljko.abstractive.zsv.manager.utils.FileUtils.getCurrentPath
import com.zeljko.abstractive.zsv.manager.utils.MeasureTime
import org.springframework.shell.command.annotation.Command
import org.springframework.shell.command.annotation.Option
import java.nio.file.Paths

@Command(command = ["zsv"], description = "Zsv commands")
open class BlobCommands(private val blobService: BlobService) {

    // zsv cat-file -f a3c241ab148df99bc5924738958c3aaad76a322b
    @Command(command = ["cat-file"], description = "Read blob object")
    @MeasureTime
    open fun decompressBlobObject(
        @Option(shortNames = ['f'], required = true, description = "Path to the file to decompress") sha: String
    ): String {
        val blob = blobService.decompress(sha, getCurrentPath()).toString()
        // remove header (blob content.length)
        return blob.substringAfter('\u0000')
    }

    // zsv hash-object -w -f src/test.txt
    @Command(command = ["hash-object"], description = "Create blob object")
    @MeasureTime
    open fun compressFileToBlobObject(
        @Option(shortNames = ['f'], required = true, description = "Path to the file to compress") fileToCompress: String
    ): String {

        val path = Paths.get(fileToCompress)
        return blobService.compressFromFile(path)
    }

}