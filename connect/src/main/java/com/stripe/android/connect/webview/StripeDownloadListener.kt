package com.stripe.android.connect.webview

import android.app.DownloadManager
import android.app.DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment.DIRECTORY_DOWNLOADS
import android.webkit.DownloadListener
import android.webkit.URLUtil
import android.widget.Toast
import androidx.core.content.FileProvider
import com.stripe.android.connect.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File

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
            while (true) {
                val cursor = downloadManager.query(query)
                cursor.moveToFirst()
                val index = cursor.getColumnIndex(DownloadManager.COLUMN_STATUS)
                if (index < 0) break // status does not exist - abort
                val status = cursor.getInt(index)
                if (status == DownloadManager.STATUS_SUCCESSFUL) {
                    showOpenFileToast(fileName, mimetype)
                    break // download complete - exit the loop
                }
                cursor.close()
                delay(1000)
            }
        }
    }

    private fun showOpenFileToast(fileName: String, mimeType: String?) {
        MainScope().launch {
            val toast = Toast.makeText(context, "Download complete", Toast.LENGTH_LONG)
            toast.view?.setOnClickListener {
                openFile(fileName, mimeType)
            }
            toast.show()
        }
    }

    private fun openFile(fileName: String, mimeType: String?) {
        val file = File(context.getExternalFilesDir(DIRECTORY_DOWNLOADS), fileName)
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val intent = Intent(Intent.ACTION_VIEW)
        intent.setDataAndType(uri, mimeType)
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        context.startActivity(intent)
    }
}
