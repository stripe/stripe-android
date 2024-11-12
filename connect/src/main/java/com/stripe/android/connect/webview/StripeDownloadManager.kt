package com.stripe.android.connect.webview

import android.app.DownloadManager
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.webkit.URLUtil

/**
 * Provides an interface for various download and file operations. Useful for mocking in tests.
 */
internal interface StripeDownloadManager {
    /**
     * Returns the ID of the download, or null if the download could not be started.
     */
    fun enqueue(request: DownloadManager.Request): Long?

    /**
     * Returns a [DownloadManager.Request] for the given URI.
     */
    fun getDownloadRequest(uri: String): DownloadManager.Request

    /**
     * Returns a best-guess file name for the given URL, content disposition, and MIME type.
     */
    fun getFileName(url: String, contentDisposition: String? = null, mimeType: String? = null): String

    /**
     * Returns a [DownloadManager.Query] for the given download ID.
     */
    fun getQueryById(id: Long): DownloadManager.Query

    /**
     * Queries the download manager for downloads matching the given query, or null if downloads can't be queried.
     */
    fun query(query: DownloadManager.Query): Cursor?
}

internal class StripeDownloadManagerImpl(context: Context) : StripeDownloadManager {
    private val downloadManager: DownloadManager? =
        context.getSystemService(Context.DOWNLOAD_SERVICE) as? DownloadManager

    override fun enqueue(request: DownloadManager.Request): Long? {
        return downloadManager?.enqueue(request)
    }

    override fun getDownloadRequest(uri: String): DownloadManager.Request {
        return DownloadManager.Request(Uri.parse(uri))
    }

    override fun getFileName(url: String, contentDisposition: String?, mimeType: String?): String {
        return URLUtil.guessFileName(url, contentDisposition, mimeType)
    }

    override fun getQueryById(id: Long): DownloadManager.Query {
        return DownloadManager.Query().setFilterById(id)
    }

    override fun query(query: DownloadManager.Query): Cursor? {
        return downloadManager?.query(query)
    }
}
