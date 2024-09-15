package com.zeljko.abstractive.zsv.manager

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path
import kotlin.test.assertEquals

class CatFileTest {

    private lateinit var zsvCommands: ZsvCommands

    @BeforeEach
    fun setup() {
        zsvCommands = ZsvCommands()
    }

    @Test
    fun testDecompressBlobObject_invalidHash() {
        val result = zsvCommands.decompressBlobObject(true, "invalidhash")
        assertEquals("Error: Invalid object hash. It must be exactly 40 characters long.", result)
    }

    @Test
    fun testDecompressBlobObject_objectDoesntExist() {
        val result = zsvCommands.decompressBlobObject(true, "a3c241ab148df99bc5924738958c3aaad76a322b")
        assertEquals("Error: Object not found.", result)
    }


    @Test
    fun testDecompressBlobObject_validBlob(@TempDir tempDir: Path) {

    }
}