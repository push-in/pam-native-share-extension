package dev.pam.shareextension

import android.app.Activity
import android.content.ContentProvider
import android.content.ContentValues
import android.content.Intent
import android.database.Cursor
import android.database.MatrixCursor
import android.net.Uri
import android.os.Bundle
import android.os.Looper
import android.os.ParcelFileDescriptor
import android.provider.OpenableColumns
import android.util.Log
import dev.pam.nativeapp.modules.ModuleCompletion
import dev.pam.nativeapp.protocol.WireMap
import dev.pam.nativeapp.protocol.WireValue
import org.json.JSONArray
import java.io.File

/** Small standalone Android harness: no account, network or product UI involved. */
class ShareDeviceTestActivity : Activity() {
    override fun onCreate(state: Bundle?) {
        super.onCreate(state)
        checked {
            if (state == null) {
                intent = Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, "same-text")
                PamIncomingShares.captureInitial(this, intent)
                PamIncomingShares.initial(ModuleCompletion { _, bytes -> checked {
                    check(Looper.myLooper() != Looper.getMainLooper())
                    val values = WireMap.decode(bytes)
                    check((values["available"] as WireValue.Flag).value)
                    firstId = (values["eventId"] as WireValue.Text).value
                    Log.i(TAG, "PASS initial copy runs away from main thread")
                    runOnUiThread { recreate() }
                } })
            } else {
                PamIncomingShares.captureInitial(this, intent)
                PamIncomingShares.initial(ModuleCompletion { _, bytes -> checked {
                    check(!(WireMap.decode(bytes)["available"] as WireValue.Flag).value)
                    Log.i(TAG, "PASS real Activity recreation does not redeliver intent")
                    val fresh = Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, "same-text")
                    receive(fresh) { values ->
                        check((values["eventId"] as WireValue.Text).value != firstId)
                        Log.i(TAG, "PASS intentional identical reshare has a new eventId")
                        photoBatch()
                    }
                } })
            }
        }
    }

    override fun onSaveInstanceState(state: Bundle) { state.putBoolean("recreated", true); super.onSaveInstanceState(state) }

    private fun receive(share: Intent, done: (Map<String, WireValue>) -> Unit) {
        PamIncomingShares.reportOpened(this, share)
        PamIncomingShares.next(ModuleCompletion { _, bytes -> checked { done(WireMap.decode(bytes)) } })
    }

    private fun share(uris: List<Uri>): Intent = Intent(Intent.ACTION_SEND_MULTIPLE).setType("image/*")
        .putParcelableArrayListExtra(Intent.EXTRA_STREAM, ArrayList(uris))

    private fun uri(name: String) = Uri.parse("content://dev.pam.sharetest.files/$name")
    private fun files(values: Map<String, WireValue>) = JSONArray((values["files"] as WireValue.Text).value)
    private fun clean(values: Map<String, WireValue>) {
        val rows = files(values)
        for (i in 0 until rows.length()) File(rows.getJSONObject(i).getString("path")).delete()
    }

    private fun photoBatch() {
        receive(share((1..25).map { uri("photo-$it") })) { values ->
            check(files(values).length() == 20)
            check(files(values).getJSONObject(0).getString("name") == "photo-1")
            clean(values)
            Log.i(TAG, "PASS 25 photos copy only first 20 in order")
            receive(share(listOf(uri("photo-1"), uri("video-2")))) { mixed ->
                check(files(mixed).length() == 1)
                clean(mixed)
                Log.i(TAG, "PASS mixed photo/video copies only first item")
                sizeFailure()
            }
        }
    }

    private fun sizeFailure() {
        PamIncomingShares.configure(mapOf("maximumBytes" to WireValue.Integer(70_000)))
        receive(share(listOf(uri("photo-1"), uri("oversize")))) { values ->
            check((values["errorCode"] as WireValue.Integer).value == 1L)
            check(File(cacheDir, "pam-incoming-shares").listFiles().orEmpty().isEmpty())
            Log.i(TAG, "PASS lying size metadata rejects stream and rolls back all files")
            PamIncomingShares.configure(mapOf("maximumItems" to WireValue.Integer(3), "maximumVideoItems" to WireValue.Integer(2)))
            receive(share((1..5).map { uri("photo-$it") })) { custom ->
                check(files(custom).length() == 3)
                clean(custom)
                Log.i(TAG, "PASS app can configure item limit before copying")
                Log.i(TAG, "ALL_PASS pssKiB=${android.os.Debug.getPss()}")
            }
        }
    }

    private fun checked(block: () -> Unit) {
        try { block() } catch (error: Throwable) { Log.e(TAG, "FAIL", error) }
    }

    private companion object {
        const val TAG = "PamShareN6"
        var firstId = ""
    }
}

class ShareDeviceTestProvider : ContentProvider() {
    override fun onCreate() = true
    override fun getType(uri: Uri) = if (uri.lastPathSegment.orEmpty().startsWith("video")) "video/mp4" else "image/png"
    override fun query(uri: Uri, projection: Array<out String>?, selection: String?, selectionArgs: Array<out String>?, sortOrder: String?): Cursor {
        check(Looper.myLooper() != Looper.getMainLooper())
        // Deliberately understate the actual size; the copy must enforce its own limit.
        return MatrixCursor(arrayOf(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE)).apply { addRow(arrayOf<Any?>(uri.lastPathSegment, 1L)) }
    }
    override fun openFile(uri: Uri, mode: String): ParcelFileDescriptor {
        check(Looper.myLooper() != Looper.getMainLooper())
        val pipes = ParcelFileDescriptor.createPipe()
        Thread {
            runCatching {
                ParcelFileDescriptor.AutoCloseOutputStream(pipes[1]).use { output ->
                    val count = if (uri.lastPathSegment == "oversize") 100_000 else 10
                    repeat(count) { output.write(it % 251) }
                }
            }
        }.start()
        return pipes[0]
    }
    override fun insert(uri: Uri, values: ContentValues?): Uri? = null
    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?) = 0
    override fun update(uri: Uri, values: ContentValues?, selection: String?, selectionArgs: Array<out String>?) = 0
}
