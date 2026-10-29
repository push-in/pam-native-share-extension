package dev.pam.shareextension

import java.io.InputStream
import java.io.OutputStream

internal enum class ShareError(val code: Int) { FILE_TOO_LARGE(1), COPY_FAILED(2), QUEUE_FULL(3) }
internal class ShareTooLargeException : java.io.IOException("Shared file exceeds maximumBytes")

internal data class ShareLimits(
    val maximumBytes: Long = 314_572_800,
    val maximumItems: Int = 20,
    val maximumVideoItems: Int = 1,
) {
    fun itemLimit(hasVideo: Boolean): Int = if (hasVideo) maximumVideoItems else maximumItems

    init {
        require(maximumBytes > 0) { "maximumBytes must be positive" }
        require(maximumItems in 1..32 && maximumVideoItems in 1..maximumItems) { "Invalid share item limits" }
    }
}

internal fun copySharedStream(input: InputStream, output: OutputStream, maximumBytes: Long): Long {
    require(maximumBytes > 0)
    val buffer = ByteArray(64 * 1024)
    var total = 0L
    while (true) {
        // Read at most one byte beyond the limit, including streams without reliable metadata.
        val count = input.read(buffer, 0, (minOf((buffer.size - 1).toLong(), maximumBytes - total) + 1).toInt())
        if (count < 0) return total
        if (count == 0) continue
        if (count.toLong() > maximumBytes - total) throw ShareTooLargeException()
        output.write(buffer, 0, count)
        total += count
    }
}

/** Save the identity and consumed marker on the original Intent before scheduling I/O. */
internal fun claimShareEvent(existingId: String?, consumed: Boolean, save: (String) -> Unit): String? {
    if (consumed) return null
    val id = existingId ?: java.util.UUID.randomUUID().toString()
    save(id)
    return id
}

internal fun copySharedFile(target: java.io.File, maximumBytes: Long, open: () -> InputStream?) {
    try {
        open().use { input ->
            requireNotNull(input) { "Cannot open shared file" }
            target.outputStream().use { output -> copySharedStream(input, output, maximumBytes) }
        }
    } catch (error: Exception) {
        target.delete()
        throw error
    }
}
