package dev.pam.shareextension

import java.io.File

/**
 * Where shared files land inside the PAM file sandbox (`filesDir/pam-files`),
 * kept apart from other imports so they can be released and expired:
 * `imports/incoming/<eventId>/incoming-…`. The share intake copies into the
 * private sandbox (a `FileReference` must resolve there), which "Clear cache"
 * never reaches: the app releases an event once it is done with it, and
 * anything older than the retention is deleted on the next start.
 */
internal object IncomingShareStorage {
    const val INCOMING_DIRECTORY = "imports/incoming"
    private const val LEGACY_DIRECTORY = "imports"
    private const val LEGACY_PREFIX = "incoming-"
    private val EVENT_ID = Regex("[A-Za-z0-9-]{1,64}")

    fun validEventId(eventId: String): Boolean = EVENT_ID.matches(eventId)

    fun eventDirectory(sandbox: File, eventId: String): File {
        require(validEventId(eventId)) { "Invalid share event id" }
        return File(File(sandbox, INCOMING_DIRECTORY), eventId)
    }

    fun relativePath(eventId: String, file: File): String = "$INCOMING_DIRECTORY/$eventId/${file.name}"

    /** Deletes every file of one share event; false when nothing was there. */
    fun release(sandbox: File, eventId: String): Boolean {
        if (!validEventId(eventId)) return false
        val directory = eventDirectory(sandbox, eventId)
        return directory.exists() && directory.deleteRecursively()
    }

    /**
     * Deletes share events (and files left by older plugin versions directly
     * in `imports/` as `incoming-*`) last modified before [now] - [maxAgeMs].
     * Returns the number of entries removed.
     */
    fun sweep(sandbox: File, now: Long, maxAgeMs: Long): Int {
        val cutoff = now - maxAgeMs
        val events = File(sandbox, INCOMING_DIRECTORY).listFiles().orEmpty()
            .filter { it.lastModified() < cutoff }
        val legacy = File(sandbox, LEGACY_DIRECTORY).listFiles().orEmpty()
            .filter { it.isFile && it.name.startsWith(LEGACY_PREFIX) && it.lastModified() < cutoff }
        return (events + legacy).count { it.deleteRecursively() }
    }
}
