package com.stripe.android.upidemo

import android.content.ContentProvider
import android.content.ContentValues
import android.database.Cursor
import android.net.Uri

/** Local demo state only; the fake bank apps simulate a backend decision here. */
class DemoStateProvider : ContentProvider() {
    override fun onCreate(): Boolean {
        DemoStore.initialize(requireNotNull(context))
        return true
    }

    override fun update(uri: Uri, values: ContentValues?, selection: String?, selectionArgs: Array<out String>?): Int {
        if (uri.pathSegments.size != 2 || uri.pathSegments.first() != "payments") return 0
        val decision = values?.getAsString("decision") ?: return 0
        return if (DemoStore.decide(uri.pathSegments[1], decision)) 1 else 0
    }

    override fun query(
        uri: Uri,
        projection: Array<out String>?,
        selection: String?,
        selectionArgs: Array<out String>?,
        sortOrder: String?,
    ): Cursor? = null

    override fun getType(uri: Uri): String = "vnd.android.cursor.item/vnd.stripe.upidemo"
    override fun insert(uri: Uri, values: ContentValues?): Uri? = null
    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int = 0
}
