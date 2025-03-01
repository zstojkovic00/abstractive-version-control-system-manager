package com.zeljko.abstractive.zsv.manager.benchmark

import java.io.File
import com.zeljko.abstractive.zsv.manager.core.services.BlobService
import com.zeljko.abstractive.zsv.manager.utils.FileUtils.getCurrentPath
import com.zeljko.abstractive.zsv.manager.utils.FileUtils.getObjectShaPath
import java.nio.file.Files
import java.nio.file.Paths
import kotlin.io.path.extension
import kotlin.io.path.fileSize
import kotlin.system.measureNanoTime

data class BenchmarkResult(
    val fileName: String,
    val fileSize: Long,
    val zsvCompressionTime: Long,
    val zsvCompressedSize: Long,
)

class Benchmark(
    private val blobService: BlobService
) {

    fun run() {
        val testFiles = Files.list(Paths.get("benchmark_files"))
            .filter { it.extension.endsWith("bin") }

        val results = mutableListOf<BenchmarkResult>()

        testFiles.forEach { file ->
            val benchmarkResult = performBenchmark(file.toFile())
            results.add(benchmarkResult)
        }

        printResults(results)
    }

    private fun performBenchmark(file: File): BenchmarkResult {
        var blobSha = ""

        val zsvCompressionTime = measureNanoTime {
            blobSha = blobService.compressFromFile(file.toPath())
        }

        val compressedFileSize = getObjectShaPath(getCurrentPath(), blobSha).fileSize()

        return BenchmarkResult(
            fileName = file.name,
            fileSize = file.length(),
            zsvCompressionTime = zsvCompressionTime,
            zsvCompressedSize = compressedFileSize
        )
    }


    private fun printResults(results: List<BenchmarkResult>) {
        println("Benchmark Results:")
        results.forEach { result ->
            println(
                """
                File: ${result.fileName}
                Original Size: ${result.fileSize} bytes
                ZSV Compression Time: ${result.zsvCompressionTime} ns
                ZSV Compression Size ${result.zsvCompressedSize} bytes
            """.trimIndent()
            )
        }
    }

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            val blobService = BlobService()
            val benchmark = Benchmark(blobService)
            benchmark.run()
        }
    }
}