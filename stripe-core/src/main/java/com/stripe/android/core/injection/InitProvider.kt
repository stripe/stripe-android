package com.stripe.android.core.injection

import android.app.Application
import android.content.ContentProvider
import android.content.ContentValues
import android.database.Cursor
import android.net.Uri

/**
 * Base class for [ContentProvider]s used for initialization purposes.
 *
 */
abstract class InitProvider : ContentProvider() {
    val application: Application
        get() = context!!.applicationContext as Application

    final override fun insert(uri: Uri, values: ContentValues?): Uri = unsupported()
    final override fun getType(uri: Uri): String = unsupported()
    final override fun query(
        uri: Uri,
        projection: Array<out String>?,
        selection: String?,
        selectionArgs: Array<out String>?,
        sortOrder: String?
    ): Cursor = unsupported()

    override fun update(
        uri: Uri,
        values: ContentValues?,
        selection: String?,
        selectionArgs: Array<out String>?
    ): Int = unsupported()

    final override fun delete(
        uri: Uri,
        selection: String?,
        selectionArgs: Array<out String>?
    ): Int = unsupported()
}

fun unsupported(errorMessage: String? = null): Nothing =
    throw UnsupportedOperationException(errorMessage)