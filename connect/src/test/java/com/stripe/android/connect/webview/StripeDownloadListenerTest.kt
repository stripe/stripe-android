package com.stripe.android.connect.webview

import android.app.DownloadManager
import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions

@ExperimentalCoroutinesApi
class StripeDownloadListenerTest {

    private val context: Context = mock {
        on { getSystemService(Context.DOWNLOAD_SERVICE) } doReturn downloadManager
        on { getString(any()) } doReturn "Placeholder string"
    }
    private val downloadManager: DownloadManager = mock {

    }
    private val dispatcher = UnconfinedTestDispatcher()

    private lateinit var stripeDownloadListener: StripeDownloadListener

    @Before
    fun setup() {
        Dispatchers.setMain(dispatcher)
        stripeDownloadListener = StripeDownloadListener(
            context = context,
            downloadManager = downloadManager,
            ioScope = MainScope(),
        )
    }

    @Test
    fun `onDownloadStart creates download request with correct parameters`() = runTest(dispatcher) {
        val url = "https://example.com/file.pdf"
        val userAgent = "Mozilla/5.0"
        val contentDisposition = "attachment; filename=file.pdf"
        val mimeType = "application/pdf"
        val contentLength = 1024L

        stripeDownloadListener.onDownloadStart(url, userAgent, contentDisposition, mimeType, contentLength)

        verify(downloadManager).enqueue(any())
    }

    @Test
    fun `onDownloadStart does nothing when URL is null`() = runTest {
        stripeDownloadListener.onDownloadStart(null, "", "", "", 0)

        verifyNoInteractions(downloadManager)
    }

    @Test
    fun `onDownloadStart does nothing when download manager doesn't exist`() = runTest {
        stripeDownloadListener = StripeDownloadListener(
            context = context,
            downloadManager = null,
            ioScope = MainScope(),
        )
        val url = "https://example.com/file.pdf"
        val userAgent = "Mozilla/5.0"
        val contentDisposition = "attachment; filename=file.pdf"
        val mimeType = "application/pdf"
        val contentLength = 1024L

        stripeDownloadListener.onDownloadStart(url, userAgent, contentDisposition, mimeType, contentLength)

        verifyNoInteractions(downloadManager)
    }
}
