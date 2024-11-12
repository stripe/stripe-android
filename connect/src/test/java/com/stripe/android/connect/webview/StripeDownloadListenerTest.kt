package com.stripe.android.connect.webview

import android.app.DownloadManager
import android.content.Context
import android.database.Cursor
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
import org.mockito.kotlin.whenever

class StripeDownloadListenerTest {

    private val downloadManagerRequest: DownloadManager.Request = mock {
        on { setDestinationInExternalPublicDir(any(), any()) } doReturn mock
        on { setNotificationVisibility(any()) } doReturn mock
        on { setTitle(any()) } doReturn mock
        on { setDescription(any()) } doReturn mock
        on { setMimeType(any()) } doReturn mock
    }
    private val context: Context = mock {
        on { getString(any()) } doReturn "Placeholder string"
    }
    private val stripeDownloadManager: StripeDownloadManager = mock {
        on { getDownloadRequest(any()) } doReturn downloadManagerRequest
        on { getFileName(any(), any(), any()) } doReturn "file.pdf"
        on { enqueue(any()) } doReturn 123L
    }
    private val stripeToastManager: StripeToastManager = mock()
    private val testScope = TestScope()

    private lateinit var stripeDownloadListener: StripeDownloadListener

    @Before
    fun setup() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        initDownloadListener()
    }

    private fun initDownloadListener(
        context: Context = this.context,
        stripeDownloadManager: StripeDownloadManager = this.stripeDownloadManager,
        stripeToastManager: StripeToastManager = this.stripeToastManager,
        ioScope: CoroutineScope = testScope,
        mainScope: CoroutineScope = testScope,
    ) {
        stripeDownloadListener = StripeDownloadListener(
            context = context,
            stripeDownloadManager = stripeDownloadManager,
            stripeToastManager = stripeToastManager,
            ioScope = ioScope,
            mainScope = mainScope,
        )
    }

    @Test
    fun `onDownloadStart creates download request`() = runTest {
        val url = "https://example.com/file.pdf"
        val userAgent = "Mozilla/5.0"
        val contentDisposition = "attachment; filename=file.pdf"
        val mimeType = "application/pdf"
        val contentLength = 1024L

        val cursor: Cursor = mock {
            on { moveToFirst() } doReturn true
            on { getColumnIndex(any()) } doReturn 0
            on { getInt(any()) } doReturn DownloadManager.STATUS_SUCCESSFUL
        }
        whenever(stripeDownloadManager.getQueryById(any())).thenReturn(mock())
        whenever(stripeDownloadManager.query(any())).thenReturn(cursor)

        stripeDownloadListener.onDownloadStart(url, userAgent, contentDisposition, mimeType, contentLength)
        testScope.testScheduler.advanceUntilIdle()

        verify(stripeDownloadManager).enqueue(any())
        verify(stripeToastManager).showToast(any())
    }

    @Test
    fun `onDownloadStart does nothing when URL is null`() = runTest {
        stripeDownloadListener.onDownloadStart(null, "", "", "", 0)
        testScope.testScheduler.advanceUntilIdle()

        verifyNoInteractions(stripeDownloadManager)
        verify(stripeToastManager).showToast(any())
    }

    @Test
    fun `onDownloadStart shows error toast when enqueue returns null`() = runTest {
        whenever(stripeDownloadManager.enqueue(any())).thenReturn(null)

        val url = "https://example.com/file.pdf"
        val userAgent = "Mozilla/5.0"
        val contentDisposition = "attachment; filename=file.pdf"
        val mimeType = "application/pdf"
        val contentLength = 1024L

        stripeDownloadListener.onDownloadStart(url, userAgent, contentDisposition, mimeType, contentLength)
        testScope.testScheduler.advanceUntilIdle()

        verify(stripeToastManager).showToast(any())
    }
}
