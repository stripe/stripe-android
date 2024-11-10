package com.stripe.android.connect.webview

import android.app.DownloadManager
import android.app.DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED
import android.app.DownloadManager.STATUS_PAUSED
import android.app.DownloadManager.STATUS_PENDING
import android.app.DownloadManager.STATUS_RUNNING
import android.content.Context
import android.net.Uri
import android.os.Environment.DIRECTORY_DOWNLOADS
import android.webkit.DownloadListener
import android.webkit.URLUtil
import android.widget.Toast
import com.stripe.android.connect.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

internal class StripeDownloadListener(
    private val context: Context,
    private val downloadManager: DownloadManager? =
        context.getSystemService(Context.DOWNLOAD_SERVICE) as? DownloadManager,
    private val ioScope: CoroutineScope = CoroutineScope(Dispatchers.IO),
) : DownloadListener {

    override fun onDownloadStart(
        url: String?,
        userAgent: String?,
        contentDisposition: String?,
        mimetype: String?,
        contentLength: Long
    ) {
        if (url == null || downloadManager == null) {
            showErrorToast()
            return
        }

        ioScope.launch {
            val request = DownloadManager.Request(Uri.parse(url))
            val fileName = URLUtil.guessFileName(url, contentDisposition, mimetype)
            request.setDestinationInExternalPublicDir(DIRECTORY_DOWNLOADS, fileName)
            request.setNotificationVisibility(VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            request.setTitle(fileName)
            request.setDescription(context.getString(R.string.stripe_downloading_file))
            request.setMimeType(mimetype)

            val downloadId = downloadManager.enqueue(request)

            // Monitor the download progress and show a toast when done
            val query = DownloadManager.Query().setFilterById(downloadId)
            var isDownloading = true
            while (isDownloading) {
                val cursor = downloadManager.query(query)
                cursor.moveToFirst()
                val index = cursor.getColumnIndex(DownloadManager.COLUMN_STATUS)
                if (index < 0) break // status does not exist - abort
                val status = cursor.getInt(index)
                if (status !in listOf(STATUS_PENDING, STATUS_RUNNING, STATUS_PAUSED)) {
                    showOpenFileToast()
                    isDownloading = false // download complete - exit the loop
                }
                cursor.close()
                delay(DOWNLOAD_DELAY_MS)
            }
        }
    }

    private fun showErrorToast() {
        MainScope().launch {
            val errorString = context.getString(R.string.stripe_unable_to_download_file)
            val toast = Toast.makeText(context, errorString, Toast.LENGTH_LONG)
            toast.show()
        }
    }

    private fun showOpenFileToast() {
        MainScope().launch {
            val downloadCompleteString = context.getString(R.string.stripe_download_complete)
            val toast = Toast.makeText(context, downloadCompleteString, Toast.LENGTH_LONG)
            toast.show()
        }
    }

    internal companion object {
        private const val DOWNLOAD_DELAY_MS = 1_000L
    }
}
