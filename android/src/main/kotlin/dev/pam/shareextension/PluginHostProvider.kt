package dev.pam.shareextension

import android.content.ContentProvider
import android.content.ContentValues
import android.database.Cursor
import android.net.Uri
import dev.pam.nativeapp.HostEvents
import dev.pam.nativeapp.HostEventListener

class PluginHostProvider : ContentProvider() {
    override fun onCreate(): Boolean {
        HostEvents.register(object : HostEventListener {
            override fun initialIntent(context: android.content.Context, intent: android.content.Intent?) = PamIncomingShares.captureInitial(context, intent)
            override fun openedIntent(context: android.content.Context, intent: android.content.Intent?) = PamIncomingShares.reportOpened(context, intent)
            override fun prepareReload() = PamIncomingShares.prepareReload()
        })
        return true
    }
    override fun query(uri: Uri, projection: Array<out String>?, selection: String?, selectionArgs: Array<out String>?, sortOrder: String?): Cursor? = null
    override fun getType(uri: Uri): String? = null
    override fun insert(uri: Uri, values: ContentValues?): Uri? = null
    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int = 0
    override fun update(uri: Uri, values: ContentValues?, selection: String?, selectionArgs: Array<out String>?): Int = 0
}
