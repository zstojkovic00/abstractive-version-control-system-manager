package com.zeljko.abstractive.zvs.manager

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class CatFileTest {
    @Test
    fun testDecompressBlobObject() {
        val objectHash = "a3c241ab148df99bc5924738958c3aaad76a322b"

        val firstTwoChars = objectHash.substring(0, 2)
        val remainingChars = objectHash.substring(2)

        assertEquals("a3", firstTwoChars)
        assertEquals("c241ab148df99bc5924738958c3aaad76a322b", remainingChars)
    }
}