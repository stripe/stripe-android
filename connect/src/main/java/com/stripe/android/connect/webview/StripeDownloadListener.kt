package com.stripe.android.connect.webview

import android.app.DownloadManager.STATUS_PAUSED
import android.app.DownloadManager.STATUS_PENDING
import android.app.DownloadManager.STATUS_RUNNING
import android.content.Context
import android.webkit.DownloadListener
import com.stripe.android.connect.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

internal class StripeDownloadListener(
    private val context: Context,
    private val stripeDownloadManager: StripeDownloadManager = StripeDownloadManagerImpl(context),
    private val stripeToastManager: StripeToastManager = StripeToastManagerImpl(context),
    private val ioScope: CoroutineScope = CoroutineScope(Dispatchers.IO),
) : DownloadListener {

    override fun onDownloadStart(
        url: String?,
        userAgent: String?,
        contentDisposition: String?,
        mimetype: String?,
        contentLength: Long
    ) {
        if (url == null) {
            showErrorToast()
            return
        }

        ioScope.launch {
            val downloadId = stripeDownloadManager.enqueueDownload(url, contentDisposition, mimetype)
            if (downloadId == null) {
                showErrorToast()
                return@launch
            }

            // Monitor the download progress and show a toast when done
            val query = stripeDownloadManager.getQueryById(downloadId)
            var isDownloading = true
            while (isDownloading) {
                val status = stripeDownloadManager.getDownloadStatus(query)
                if (status == null) {
                    showErrorToast()
                    return@launch
                } else if (status !in listOf(STATUS_PENDING, STATUS_RUNNING, STATUS_PAUSED)) {
                    showOpenFileToast()
                    isDownloading = false // download complete - exit the loop
                }
                delay(DOWNLOAD_DELAY_MS)
            }
        }
    }

    private fun showErrorToast() {
        stripeToastManager.showToast(context.getString(R.string.stripe_unable_to_download_file))
    }

    private fun showOpenFileToast() {
        stripeToastManager.showToast(context.getString(R.string.stripe_download_complete))
    }

    internal companion object {
        private const val DOWNLOAD_DELAY_MS = 1_000L
    }
}
