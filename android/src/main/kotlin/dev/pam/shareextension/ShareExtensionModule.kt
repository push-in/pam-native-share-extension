package dev.pam.shareextension

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import dev.pam.nativeapp.modules.*
import dev.pam.nativeapp.protocol.*
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.UUID
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit

/** Legacy inbox API; the app-facing IncomingShares API owns the host intent queue. */
class ShareExtensionModule(context: Context) : NativeModule {
    private val activity = context as? Activity
    private val root = File(context.filesDir, "pam-share-inbox")
    private val worker = ThreadPoolExecutor(0, 1, 1, TimeUnit.SECONDS, ArrayBlockingQueue<Runnable>(1)) {
        task -> Thread(task, "pam-share-inbox")
    }

    override fun invoke(method: String, payload: ByteArray, completion: ModuleCompletion) {
        if (method != "drain") {
            completion.fail("Unknown method: $method")
            return
        }
        runCatching {
            worker.execute {
                runCatching { drain() }
                    .onSuccess { completion.complete(ModuleResultStatus.SUCCESS, WireMap.encode(mapOf("json" to WireValue.Text(it.toString())))) }
                    .onFailure { completion.fail(it.message.orEmpty()) }
            }
        }.onFailure { completion.fail(it.message.orEmpty()) }
    }

    @Suppress("DEPRECATION")
    private fun drain(): JSONArray {
        val output = JSONArray()
        val intent = activity?.intent ?: return output
        if (intent.action != Intent.ACTION_SEND && intent.action != Intent.ACTION_SEND_MULTIPLE) return output
        synchronized(intent) {
            claimShareEvent(intent.getStringExtra(EVENT_ID), intent.getBooleanExtra(CONSUMED, false)) { id ->
                intent.putExtra(EVENT_ID, id)
                intent.putExtra(CONSUMED, true)
            } ?: return output
        }
        val now = System.currentTimeMillis()
        intent.getStringExtra(Intent.EXTRA_TEXT)?.takeIf { it.isNotBlank() }?.let {
            output.put(row(if (Uri.parse(it).scheme in listOf("http", "https")) 2 else 1, it, "text/plain", now))
        }
        val uris = linkedSetOf<Uri>()
        intent.clipData?.let { clip -> for (i in 0 until clip.itemCount) clip.getItemAt(i).uri?.let(uris::add) }
        (intent.getParcelableExtra<android.os.Parcelable>(Intent.EXTRA_STREAM) as? Uri)?.let(uris::add)
        intent.getParcelableArrayListExtra<android.os.Parcelable>(Intent.EXTRA_STREAM)?.filterIsInstance<Uri>()?.let(uris::addAll)
        val limits = ShareLimits()
        val video = intent.type.orEmpty().startsWith("video/") || uris.any { activity.contentResolver.getType(it).orEmpty().startsWith("video/") }
        val copied = mutableListOf<File>()
        try {
            root.mkdirs()
            uris.take(limits.itemLimit(video)).forEach { uri ->
                val mime = activity.contentResolver.getType(uri) ?: "application/octet-stream"
                val file = File(root, UUID.randomUUID().toString())
                copied.add(file)
                copySharedFile(file, limits.maximumBytes) { activity.contentResolver.openInputStream(uri) }
                output.put(row(3, file.name, mime, now))
            }
        } catch (error: Exception) {
            copied.forEach { it.delete() }
            throw error
        }
        return output
    }

    private fun row(kind: Int, value: String, mime: String, time: Long) = JSONObject()
        .put("id", UUID.randomUUID().toString()).put("kind", kind).put("value", value.take(8192))
        .put("mimeType", mime.take(255)).put("createdAtMillis", time)

    private fun ModuleCompletion.fail(message: String) = complete(ModuleResultStatus.FAILURE, message.take(1024).toByteArray())

    private companion object {
        const val EVENT_ID = "dev.pam.shareextension.EVENT_ID"
        const val CONSUMED = "dev.pam.shareextension.CONSUMED"
    }
}
