package com.stripe.android.connect.webview

import android.app.DownloadManager
import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.TestScope
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

class StripeDownloadListenerTest {

    private val downloadManager: DownloadManager = mock {
        on { enqueue(any()) } doReturn 123L
    }
    private val downloadManagerRequest: DownloadManager.Request = mock {
        // mock all builder methods to return this mock, for proper chaining
        on { setDestinationInExternalPublicDir(any(), any()) } doReturn mock
        on { setNotificationVisibility(any()) } doReturn mock
        on { setTitle(any()) } doReturn mock
        on { setDescription(any()) } doReturn mock
        on { setMimeType(any()) } doReturn mock
    }
    private val context: Context = mock {
        on { getSystemService(Context.DOWNLOAD_SERVICE) } doReturn downloadManager
        on { getString(any()) } doReturn "Placeholder string"
    }
    private val testScope = TestScope()

    private lateinit var stripeDownloadListener: StripeDownloadListener

    @Before
    fun setup() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        initDownloadListener()
    }

    private fun initDownloadListener(
        context: Context = this.context,
        downloadManager: DownloadManager? = this.downloadManager,
        ioScope: CoroutineScope = testScope,
    ) {
        stripeDownloadListener = StripeDownloadListener(
            context = context,
            downloadManager = downloadManager,
            ioScope = ioScope,
            getDownloadManagerRequest = { downloadManagerRequest }, // mock request
            getFileName = { _, _, _ -> "file.pdf" }, // fake file name for testing
            parseUri = { mock() }, // mock Uri.parse
            showToast = { }, // no-op toast for testing
        )
    }

    @Test
    fun `onDownloadStart creates download request`() = runTest {
        val url = "https://example.com/file.pdf"
        val userAgent = "Mozilla/5.0"
        val contentDisposition = "attachment; filename=file.pdf"
        val mimeType = "application/pdf"
        val contentLength = 1024L

        stripeDownloadListener.onDownloadStart(url, userAgent, contentDisposition, mimeType, contentLength)
        testScope.testScheduler.advanceUntilIdle()

        verify(downloadManager).enqueue(any())
    }

    @Test
    fun `onDownloadStart does nothing when URL is null`() = runTest {
        stripeDownloadListener.onDownloadStart(null, "", "", "", 0)
        testScope.testScheduler.advanceUntilIdle()

        verifyNoInteractions(downloadManager)
    }

    @Test
    fun `onDownloadStart does nothing when download manager doesn't exist`() = runTest {
        initDownloadListener(downloadManager = null)

        val url = "https://example.com/file.pdf"
        val userAgent = "Mozilla/5.0"
        val contentDisposition = "attachment; filename=file.pdf"
        val mimeType = "application/pdf"
        val contentLength = 1024L

        stripeDownloadListener.onDownloadStart(url, userAgent, contentDisposition, mimeType, contentLength)
        testScope.testScheduler.advanceUntilIdle()

        verifyNoInteractions(downloadManager)
    }
}
