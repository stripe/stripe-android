package com.stripe.android.connect.webview

import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.os.Environment
import android.webkit.DownloadListener
import android.webkit.URLUtil
import com.stripe.android.connect.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class StripeDownloadListener(
    private val context: Context,
    private val ioScope: CoroutineScope = CoroutineScope(Dispatchers.IO),
) : DownloadListener {

    private val downloadManager: DownloadManager by lazy {
        context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
    }

    override fun onDownloadStart(
        url: String?,
        userAgent: String?,
        contentDisposition: String?,
        mimetype: String?,
        contentLength: Long
    ) {
        url ?: return
        val request = DownloadManager.Request(Uri.parse(url))

        request.setDestinationInExternalPublicDir(
            Environment.DIRECTORY_DOWNLOADS,
            URLUtil.guessFileName(url, contentDisposition, mimetype)
        )
        request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
        request.setTitle(URLUtil.guessFileName(url, contentDisposition, mimetype))
        request.setDescription(context.getString(R.string.stripe_downloading_file))
        request.setMimeType(mimetype)

        ioScope.launch {
            downloadManager.enqueue(request)
        }
    }
}
