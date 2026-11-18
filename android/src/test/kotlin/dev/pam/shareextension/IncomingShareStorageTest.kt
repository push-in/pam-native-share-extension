package dev.pam.shareextension

import java.io.File
import java.nio.file.Files
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class IncomingShareStorageTest {
    private val sandbox: File = Files.createTempDirectory("pam-files").toFile()

    private fun file(path: String, modified: Long): File =
        File(sandbox, path).apply {
            parentFile.mkdirs()
            writeText("x")
            setLastModified(modified)
        }

    @Test fun eachEventHasItsOwnDirectoryUnderImportsIncoming() {
        val directory = IncomingShareStorage.eventDirectory(sandbox, "4f1c-77")
        assertEquals(File(sandbox, "imports/incoming/4f1c-77"), directory)
        assertEquals("imports/incoming/4f1c-77/incoming-1-a.jpg", IncomingShareStorage.relativePath("4f1c-77", File(directory, "incoming-1-a.jpg")))
        assertFalse(IncomingShareStorage.validEventId("../x"))
        assertFalse(IncomingShareStorage.validEventId(""))
    }

    @Test fun releaseDeletesOnlyThatEvent() {
        file("imports/incoming/a-1/incoming-1-a.jpg", 1_000)
        file("imports/incoming/b-2/incoming-2-b.mp4", 1_000)
        assertTrue(IncomingShareStorage.release(sandbox, "a-1"))
        assertFalse(File(sandbox, "imports/incoming/a-1").exists())
        assertTrue(File(sandbox, "imports/incoming/b-2/incoming-2-b.mp4").exists())
        assertFalse(IncomingShareStorage.release(sandbox, "a-1"))
        assertFalse(IncomingShareStorage.release(sandbox, "../../shared_prefs"))
    }

    @Test fun sweepDeletesExpiredEventsAndLegacyCopiesOnly() {
        val now = 1_000L * 24 * 3_600_000
        val day = 24L * 3_600_000
        file("imports/incoming/old/incoming-1-a.jpg", now - day - 1).parentFile.setLastModified(now - day - 1)
        file("imports/incoming/new/incoming-2-b.jpg", now - 60_000).parentFile.setLastModified(now - 60_000)
        file("imports/incoming-3-legacy.mp4", now - day - 1)
        file("imports/incoming-4-recent.mp4", now - 60_000)
        file("imports/gallery-5.jpg", now - day * 30)
        file("edits/incoming-6.jpg", now - day * 30)
        assertEquals(2, IncomingShareStorage.sweep(sandbox, now, day))
        assertFalse(File(sandbox, "imports/incoming/old").exists())
        assertFalse(File(sandbox, "imports/incoming-3-legacy.mp4").exists())
        assertTrue(File(sandbox, "imports/incoming/new/incoming-2-b.jpg").exists())
        assertTrue(File(sandbox, "imports/incoming-4-recent.mp4").exists())
        assertTrue(File(sandbox, "imports/gallery-5.jpg").exists())
        assertTrue(File(sandbox, "edits/incoming-6.jpg").exists())
    }
}
