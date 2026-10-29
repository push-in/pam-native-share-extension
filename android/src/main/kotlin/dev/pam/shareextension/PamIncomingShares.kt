package dev.pam.shareextension

import dev.pam.nativeapp.modules.*

import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.database.Cursor
import android.net.Uri
import android.provider.OpenableColumns
import dev.pam.nativeapp.protocol.WireMap
import dev.pam.nativeapp.protocol.WireValue
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.ArrayDeque

internal object PamIncomingShares {
    private val lock = Any()
    private var waiter: ModuleCompletion? = null

    private data class Request(val context: Context, val intent: Intent, val eventId: String)
    private var initialRequest: Request? = null
    private val requests = ArrayDeque<Request>()
    private var limits = ShareLimits()
    private val worker = java.util.concurrent.ThreadPoolExecutor(
        0, 1, 1, java.util.concurrent.TimeUnit.SECONDS,
        java.util.concurrent.ArrayBlockingQueue<Runnable>(MAX_QUEUED_EVENTS),
        { task -> Thread(task, "pam-incoming-share") },
    )

    private fun claim(context: Context, intent: Intent?): Request? {
        if (intent == null || intent.action !in setOf(Intent.ACTION_SEND, Intent.ACTION_SEND_MULTIPLE)) return null
        synchronized(intent) {
            val id = claimShareEvent(intent.getStringExtra(EVENT_ID), intent.getBooleanExtra(CONSUMED, false)) { value ->
                intent.putExtra(EVENT_ID, value)
                intent.putExtra(CONSUMED, true)
            } ?: return null
            return Request(context.applicationContext, Intent(intent), id)
        }
    }

    fun captureInitial(context: Context, intent: Intent?) {
        synchronized(lock) {
            val request = claim(context, intent) ?: return
            if (initialRequest == null) initialRequest = request
        }
    }

    fun reportOpened(context: Context, intent: Intent?) {
        synchronized(lock) {
            val request = claim(context, intent) ?: return
            val pending = waiter
            if (pending == null) {
                if (requests.size >= MAX_QUEUED_EVENTS) requests.removeFirst()
                requests.addLast(request)
            } else {
                waiter = null
                deliver(request, pending)
            }
        }
    }

    fun configure(values: Map<String, WireValue>) {
        fun number(key: String, fallback: Long) = (values[key] as? WireValue.Integer)?.value ?: fallback
        synchronized(lock) {
            val items = number("maximumItems", limits.maximumItems.toLong())
            val videoItems = number("maximumVideoItems", limits.maximumVideoItems.toLong())
            require(items in 1L..32L && videoItems in 1L..items) { "Invalid share item limits" }
            limits = ShareLimits(number("maximumBytes", limits.maximumBytes), items.toInt(), videoItems.toInt())
        }
    }

    private fun deliver(request: Request, completion: ModuleCompletion) {
        val policy = limits
        try {
            worker.execute {
                val payload = try {
                    capture(request.context, request.intent, request.eventId, policy) ?: emptyPayload()
                } catch (error: Exception) {
                    WireMap.encode(mapOf(
                        "available" to WireValue.Flag(false),
                        "eventId" to WireValue.Text(request.eventId),
                        "errorCode" to WireValue.Integer((if (error is ShareTooLargeException) ShareError.FILE_TOO_LARGE else ShareError.COPY_FAILED).code.toLong()),
                    ))
                }
                completion.complete(ModuleResultStatus.SUCCESS, payload)
            }
        } catch (_: java.util.concurrent.RejectedExecutionException) {
            completion.complete(ModuleResultStatus.SUCCESS, WireMap.encode(mapOf(
                "available" to WireValue.Flag(false),
                "eventId" to WireValue.Text(request.eventId),
                "errorCode" to WireValue.Integer(ShareError.QUEUE_FULL.code.toLong()),
            )))
        }
    }

    fun initial(completion: ModuleCompletion) {
        synchronized(lock) {
            val request = initialRequest
            initialRequest = null
            if (request != null) deliver(request, completion)
            else completion.complete(ModuleResultStatus.SUCCESS, emptyPayload())
        }
    }

    fun next(completion: ModuleCompletion) {
        synchronized(lock) {
            require(waiter == null) { "Only one incoming-share listener can wait at a time" }
            if (requests.isEmpty()) waiter = completion
            else deliver(requests.removeFirst(), completion)
        }
    }

    fun close(message: String) {
        val pending = synchronized(lock) {
            val value = waiter
            waiter = null
            initialRequest = null
            requests.clear()
            value
        }
        pending?.complete(ModuleResultStatus.FAILURE, message.toByteArray())
    }

    fun prepareReload() {
        synchronized(lock) { waiter = null }
    }

    private fun capture(context: Context, intent: Intent, eventId: String, policy: ShareLimits): ByteArray? {
        val uris = linkedSetOf<Uri>()
        intent.clipData?.appendUris(uris)
        if (intent.action == Intent.ACTION_SEND_MULTIPLE) {
            intent.parcelableUriList(Intent.EXTRA_STREAM).forEach(uris::add)
        } else {
            intent.parcelableUri(Intent.EXTRA_STREAM)?.let(uris::add)
        }
        val files = JSONArray()
        val hasVideo = intent.type.orEmpty().startsWith("video/", ignoreCase = true) ||
            uris.any { context.contentResolver.getType(it).orEmpty().startsWith("video/", ignoreCase = true) }
        val selected = uris.take(policy.itemLimit(hasVideo))
        val copied = mutableListOf<File>()
        try {
            selected.forEach { uri -> files.put(copyIntoSandbox(context, uri, intent.type, policy, copied)) }
        } catch (error: Exception) {
            copied.forEach { it.delete() }
            throw error
        }
        val text = intent.getStringExtra(Intent.EXTRA_TEXT).orEmpty().take(MAX_TEXT_BYTES)
        val subject = intent.getStringExtra(Intent.EXTRA_SUBJECT).orEmpty().take(MAX_SUBJECT_BYTES)
        if (files.length() == 0 && text.isBlank() && subject.isBlank()) return null

        return WireMap.encode(
            mapOf(
                "available" to WireValue.Flag(true),
                "eventId" to WireValue.Text(eventId),
                "text" to WireValue.Text(text),
                "subject" to WireValue.Text(subject),
                "mimeType" to WireValue.Text(intent.type.orEmpty()),
                "files" to WireValue.Text(files.toString()),
            ),
        )
    }

    private fun copyIntoSandbox(context: Context, uri: Uri, fallbackMime: String?, policy: ShareLimits, copied: MutableList<File>): JSONObject {
        val metadata = queryMetadata(context, uri)
        if (metadata.second > policy.maximumBytes) throw ShareTooLargeException()
        val mime = context.contentResolver.getType(uri)
            ?: fallbackMime
            ?: "application/octet-stream"
        val directory = File(context.cacheDir, "pam-incoming-shares").apply { mkdirs() }
        val safeName = metadata.first
            .replace(Regex("[^A-Za-z0-9._-]"), "_")
            .take(120)
            .ifBlank { "shared-${System.nanoTime()}" }
        val target = File.createTempFile("incoming-", "-$safeName", directory)
        copied.add(target)
        copySharedFile(target, policy.maximumBytes) { context.contentResolver.openInputStream(uri) }
        return JSONObject()
            .put("path", target.absolutePath)
            .put("name", metadata.first)
            .put("mimeType", mime)
            .put("size", target.length())
    }

    private fun queryMetadata(context: Context, uri: Uri): Pair<String, Long> {
        var name = uri.lastPathSegment?.substringAfterLast('/').orEmpty()
        var size = 0L
        val cursor: Cursor? = context.contentResolver.query(
            uri,
            arrayOf(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE),
            null,
            null,
            null,
        )
        cursor?.use {
            if (it.moveToFirst()) {
                val nameIndex = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                val sizeIndex = it.getColumnIndex(OpenableColumns.SIZE)
                if (nameIndex >= 0) name = it.getString(nameIndex).orEmpty()
                if (sizeIndex >= 0 && !it.isNull(sizeIndex)) size = it.getLong(sizeIndex)
            }
        }
        return (name.ifBlank { "shared-file" }) to size
    }

    private fun ClipData.appendUris(target: MutableSet<Uri>) {
        repeat(itemCount) { index -> getItemAt(index).uri?.let(target::add) }
    }

    @Suppress("DEPRECATION")
    private fun Intent.parcelableUri(key: String): Uri? =
        if (android.os.Build.VERSION.SDK_INT >= 33) getParcelableExtra(key, Uri::class.java)
        else getParcelableExtra(key)

    @Suppress("DEPRECATION")
    private fun Intent.parcelableUriList(key: String): List<Uri> =
        if (android.os.Build.VERSION.SDK_INT >= 33) {
            getParcelableArrayListExtra(key, Uri::class.java).orEmpty()
        } else {
            getParcelableArrayListExtra<Uri>(key).orEmpty()
        }

    private fun emptyPayload(): ByteArray = WireMap.encode(
        mapOf(
            "available" to WireValue.Flag(false),
            "text" to WireValue.Text(""),
            "subject" to WireValue.Text(""),
            "mimeType" to WireValue.Text(""),
            "files" to WireValue.Text("[]"),
        ),
    )

    private const val MAX_QUEUED_EVENTS = 16
    private const val EVENT_ID = "dev.pam.shareextension.EVENT_ID"
    private const val CONSUMED = "dev.pam.shareextension.CONSUMED"
    private const val MAX_TEXT_BYTES = 65_536
    private const val MAX_SUBJECT_BYTES = 4_096
}
