package dev.pam.shareextension

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import org.junit.Assert.*
import org.junit.Test

class ShareLimitsTest {
    @Test fun streamAcceptsExactLimitAndPreservesBytes() {
        val bytes = ByteArray(150_000) { (it % 251).toByte() }
        val output = ByteArrayOutputStream()
        assertEquals(bytes.size.toLong(), copySharedStream(ByteArrayInputStream(bytes), output, bytes.size.toLong()))
        assertArrayEquals(bytes, output.toByteArray())
    }

    @Test fun oversizedUnknownLengthStreamNeverWritesBeyondLimit() {
        val output = ByteArrayOutputStream()
        try {
            copySharedStream(ByteArrayInputStream(ByteArray(200_000)), output, 70_000)
            fail("Expected typed size error")
        } catch (_: ShareTooLargeException) {
            assertTrue(output.size() <= 70_000)
        }
    }

    @Test fun largeLimitDoesNotOverflowAndEmptyStreamWorks() {
        assertEquals(0L, copySharedStream(ByteArrayInputStream(byteArrayOf()), ByteArrayOutputStream(), Long.MAX_VALUE))
    }

    @Test fun identitySurvivesIntentRecreationButDeliberateReshareIsNew() {
        var savedId: String? = null
        var consumed = false
        val first = claimShareEvent(savedId, consumed) { savedId = it; consumed = true }
        assertNotNull(first)
        assertEquals(first, savedId)
        assertNull(claimShareEvent(savedId, consumed) { fail("Already consumed") })
        assertNotEquals(first, claimShareEvent(null, false) {})
        assertEquals("restored", claimShareEvent("restored", false) {})
    }

    @Test fun failedCopiesRemovePartialTargetsAndCloseInput() {
        val target = java.io.File.createTempFile("share-test-", ".bin")
        var closed = false
        val input = object : ByteArrayInputStream(ByteArray(100_000)) {
            override fun close() { closed = true; super.close() }
        }
        try {
            copySharedFile(target, 70_000) { input }
            fail("Expected size error")
        } catch (_: ShareTooLargeException) {
            assertFalse(target.exists())
            assertTrue(closed)
        } finally { target.delete() }
    }

    @Test fun videoPolicyCapsMixedSharesBeforeCopying() {
        val files = (1..25).toList()
        assertEquals((1..20).toList(), files.take(ShareLimits().itemLimit(false)))
        assertEquals(listOf(1), files.take(ShareLimits().itemLimit(true)))
        assertEquals(listOf(1, 2), files.take(ShareLimits(10, 5, 2).itemLimit(true)))
    }

    @Test fun defaultsAndConfigurableLimits() {
        assertEquals(314_572_800L, ShareLimits().maximumBytes)
        assertEquals(20, ShareLimits().maximumItems)
        assertEquals(1, ShareLimits().maximumVideoItems)
        assertEquals(2, ShareLimits(10, 5, 2).maximumVideoItems)
        try { ShareLimits(0); fail("Expected invalid limit") } catch (_: IllegalArgumentException) {}
        try { ShareLimits(1, 33); fail("Expected invalid limit") } catch (_: IllegalArgumentException) {}
        try { ShareLimits(1, 2, 3); fail("Expected invalid limit") } catch (_: IllegalArgumentException) {}
    }
}
