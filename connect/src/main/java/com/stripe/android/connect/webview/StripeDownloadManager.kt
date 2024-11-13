package com.stripe.android.connect.webview

import android.app.DownloadManager
import android.app.DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED
import android.content.Context
import android.net.Uri
import android.os.Environment.DIRECTORY_DOWNLOADS
import android.webkit.URLUtil
import com.stripe.android.connect.R

/**
 * Provides an interface for various download and file operations. Useful for mocking in tests.
 */
internal interface StripeDownloadManager {

    /**
     * Enqueues a download for the given URL, content disposition, and MIME type.
     * Returns null if a download could not be started.
     */
    fun enqueueDownload(url: String, contentDisposition: String? = null, mimeType: String? = null): Long?

    /**
     * Returns a [DownloadManager.Query] for the given download ID, used for monitoring the status of a download.
     */
    fun getQueryById(id: Long): DownloadManager.Query

    /**
     * Returns the status of the download represented by [query]. Maps to [DownloadManager.COLUMN_STATUS], ie.
     * [DownloadManager.STATUS_PENDING], [DownloadManager.STATUS_RUNNING], [DownloadManager.STATUS_PAUSED], etc.
     *
     * Returns null if the status could not be determined. This operation should not be retried if null is returned.
     */
    fun getDownloadStatus(query: DownloadManager.Query): Int?
}

internal class StripeDownloadManagerImpl(private val context: Context) : StripeDownloadManager {
    private val downloadManager: DownloadManager? =
        context.getSystemService(Context.DOWNLOAD_SERVICE) as? DownloadManager

    override fun enqueueDownload(url: String, contentDisposition: String?, mimeType: String?): Long? {
        downloadManager ?: return null

        val request = DownloadManager.Request(Uri.parse(url))
        val fileName = URLUtil.guessFileName(url, contentDisposition, mimeType)
        request.setDestinationInExternalPublicDir(DIRECTORY_DOWNLOADS, fileName)
        request.setNotificationVisibility(VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
        request.setTitle(fileName)
        request.setDescription(context.getString(R.string.stripe_downloading_file))
        request.setMimeType(mimeType)

        return downloadManager.enqueue(request)
    }

    override fun getQueryById(id: Long): DownloadManager.Query {
        return DownloadManager.Query().setFilterById(id)
    }

    override fun getDownloadStatus(query: DownloadManager.Query): Int? {
        downloadManager ?: return null

        val cursor = downloadManager.query(query) ?: return null
        return cursor.use { resource ->
            resource.moveToFirst()
            val index = resource.getColumnIndex(DownloadManager.COLUMN_STATUS)
            if (index < 0) return null // status does not exist - abort
            resource.getInt(index)
        }
    }
}
