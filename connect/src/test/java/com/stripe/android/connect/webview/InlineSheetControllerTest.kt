package com.stripe.android.connect.webview

import android.net.Uri
import android.os.Looper
import com.google.common.truth.Truth.assertThat
import com.stripe.android.connect.webview.InlineSheet.State
import com.stripe.android.core.Logger
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf

@RunWith(RobolectricTestRunner::class)
class InlineSheetControllerTest {

    private val callbacks: InlineSheetController.Callbacks = mock()
    private lateinit var listener: StripeConnectSheetWebView.Listener
    private val controller = InlineSheetController(
        componentUrl = Uri.parse("https://connect-js.stripe.com/v1.0/android_webview.html#component=x"),
        callbacks = callbacks,
        logger = Logger.noop(),
        createWebView = {
            listener = it
            mock()
        },
    )

    @Test
    fun `a sheet is presented only after sheetChanged`() {
        openSheet()
        assertThat(controller.sheetsFlow.value.single().state).isEqualTo(State.STAGED)

        controller.onSheetChanged(SHEET_ID, 612f, isDismissible = true)

        assertThat(controller.sheetsFlow.value.single().state).isEqualTo(State.SHOWING)
    }

    @Test
    fun `dismissing a dismissible sheet sends sheetClosed and releases it`() {
        val webView = openSheet()
        controller.onSheetChanged(SHEET_ID, 612f, isDismissible = true)

        controller.onSheetDismissed(SHEET_ID)

        verify(callbacks).onSheetClosed(SHEET_ID)
        assertThat(controller.sheetsFlow.value).isEmpty()
        shadowOf(Looper.getMainLooper()).idle()
        verify(webView).destroy()
    }

    @Test
    fun `closing a non-dismissible sheet sends sheetCloseRequested and keeps it open`() {
        openSheet()
        controller.onSheetChanged(SHEET_ID, 612f, isDismissible = false)

        controller.onSheetCloseRequested(SHEET_ID)

        verify(callbacks).onSheetCloseRequested(SHEET_ID)
        verify(callbacks, never()).onSheetClosed(any())
        assertThat(controller.sheetsFlow.value.single().state).isEqualTo(State.SHOWING)
    }

    @Test
    fun `a sheet is released when its window closes`() {
        val webView = openSheet()
        controller.onSheetChanged(SHEET_ID, 612f, isDismissible = false)

        listener.onWindowClosed(webView)
        controller.onSheetReleased(SHEET_ID)

        verify(callbacks).onSheetClosed(SHEET_ID)
        assertThat(controller.sheetsFlow.value).isEmpty()
        shadowOf(Looper.getMainLooper()).idle()
        verify(webView).destroy()
    }

    @Test
    fun `a window that isn't a sheet opens outside the app only when a user gesture opened it`() {
        val url = Uri.parse("https://example.com/help")
        val withoutGesture = controller.createWindow(isUserGesture = false) as StripeConnectSheetWebView
        val withGesture = controller.createWindow(isUserGesture = true) as StripeConnectSheetWebView

        assertThat(listener.onWindowNavigated(withoutGesture, url)).isFalse()
        verify(callbacks, never()).openOutsideSheet(any())
        assertThat(listener.onWindowNavigated(withGesture, url)).isFalse()
        verify(callbacks).openOutsideSheet(url)
        assertThat(controller.sheetsFlow.value).isEmpty()
    }

    private fun openSheet(): StripeConnectSheetWebView {
        val webView = controller.createWindow(isUserGesture = false) as StripeConnectSheetWebView
        val url = Uri.parse(
            "https://connect-js.stripe.com/accessory_layer_abc123.html#os_sheet=true&os_sheet_id=$SHEET_ID"
        )
        assertThat(listener.onWindowNavigated(webView, url)).isTrue()
        return webView
    }

    private companion object {
        const val SHEET_ID = "sheet-1"
    }
}
