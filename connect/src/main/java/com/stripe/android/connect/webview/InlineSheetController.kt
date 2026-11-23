package com.stripe.android.connect.webview

import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.view.ViewGroup
import android.webkit.PermissionRequest
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebView
import com.stripe.android.connect.webview.InlineSheet.State
import com.stripe.android.core.Logger
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * Tracks the windows an inline component's web view opens and decides which become native sheets.
 *
 * A window becomes a sheet only if its first document is an accessory layer page on the Connect.js origin, and
 * it's shown only once Connect.js reports its height with `sheetChanged`. Must be used on the main thread.
 */
internal class InlineSheetController(
    private val componentUrl: Uri,
    private val callbacks: Callbacks,
    private val logger: Logger,
    private val createWebView: (StripeConnectSheetWebView.Listener) -> StripeConnectSheetWebView,
) {
    interface Callbacks {
        /**
         * Tells Connect.js that a sheet went away, so it can run its dialog's close logic.
         */
        fun onSheetClosed(sheetId: String)

        /**
         * Tells Connect.js the user tried to close a sheet that isn't dismissible.
         */
        fun onSheetCloseRequested(sheetId: String)

        /**
         * Opens a window that isn't a sheet the way the component opens links today.
         */
        fun openOutsideSheet(url: Uri)

        fun onShowFileChooser(
            webView: WebView,
            filePathCallback: ValueCallback<Array<Uri>>,
            fileChooserParams: WebChromeClient.FileChooserParams,
        ): Boolean

        fun onPermissionRequest(webView: WebView, request: PermissionRequest)
    }

    private val loggerTag = javaClass.simpleName
    private val mainHandler = Handler(Looper.getMainLooper())

    private val _sheetsFlow = MutableStateFlow<List<InlineSheet>>(emptyList())

    /**
     * Sheets in the order their windows opened, so later sheets stack on top.
     */
    val sheetsFlow: StateFlow<List<InlineSheet>> get() = _sheetsFlow.asStateFlow()

    // Windows whose first document isn't known yet, and whether a user gesture opened them.
    private val pendingWindows = mutableMapOf<StripeConnectSheetWebView, Boolean>()
    private val sheetWebViews = mutableMapOf<String, StripeConnectSheetWebView>()

    /**
     * Returns a web view for a window the component's web view is opening.
     */
    fun createWindow(isUserGesture: Boolean): WebView {
        val webView = createWebView(windowListener)
        pendingWindows[webView] = isUserGesture
        return webView
    }

    fun sheetWebView(sheetId: String): StripeConnectSheetWebView? = sheetWebViews[sheetId]

    /**
     * Shows a staged sheet, or updates a shown one, at [height] CSS pixels.
     */
    fun onSheetChanged(sheetId: String, height: Float, isDismissible: Boolean) {
        val sheet = findSheet(sheetId) ?: return
        if (sheet.state == State.CLOSING) return
        updateSheet(sheet.copy(state = State.SHOWING, height = height, isDismissible = isDismissible))
    }

    /**
     * Called once the user dismissed a dismissible sheet and it's off screen.
     */
    fun onSheetDismissed(sheetId: String) {
        val sheet = findSheet(sheetId) ?: return
        if (sheet.state != State.SHOWING) return
        removeSheet(sheetId)
        callbacks.onSheetClosed(sheetId)
    }

    /**
     * Called when the user tries to close a sheet that isn't dismissible. It stays open until Connect.js closes it.
     */
    fun onSheetCloseRequested(sheetId: String) {
        val sheet = findSheet(sheetId) ?: return
        if (sheet.state != State.SHOWING || sheet.isDismissible) return
        callbacks.onSheetCloseRequested(sheetId)
    }

    /**
     * Called once a closing sheet is off screen.
     */
    fun onSheetReleased(sheetId: String) {
        if (findSheet(sheetId)?.state == State.CLOSING) {
            removeSheet(sheetId)
        }
    }

    /**
     * Called when the screen showing the component goes away, which takes its sheets with it.
     */
    fun onHostDismissed() {
        _sheetsFlow.value
            .filter { it.state == State.STAGED || it.state == State.SHOWING }
            .forEach { sheet ->
                removeSheet(sheet.id)
                callbacks.onSheetClosed(sheet.id)
            }
    }

    /**
     * Destroys every window, for when the component's document goes away.
     */
    fun reset() {
        pendingWindows.keys.forEach(::destroyLater)
        pendingWindows.clear()
        sheetWebViews.values.forEach(::destroyLater)
        sheetWebViews.clear()
        _sheetsFlow.value = emptyList()
    }

    private val windowListener = object : StripeConnectSheetWebView.Listener {
        override fun onWindowNavigated(webView: StripeConnectSheetWebView, url: Uri): Boolean {
            val isUserGesture = pendingWindows.remove(webView) ?: return false
            val sheetId = InlineContract.sheetIdFromWindowUrl(url, componentUrl)
            if (sheetId == null || sheetWebViews.containsKey(sheetId)) {
                logger.debug("($loggerTag) Dropping a window that isn't a new sheet")
                if (sheetId == null && isUserGesture) {
                    callbacks.openOutsideSheet(url)
                }
                destroyLater(webView)
                return false
            }

            sheetWebViews[sheetId] = webView
            _sheetsFlow.update {
                it + InlineSheet(id = sheetId, state = State.STAGED, height = null, isDismissible = true)
            }
            return true
        }

        override fun isSheetUrl(url: Uri): Boolean {
            return InlineContract.sheetIdFromWindowUrl(url, componentUrl) != null
        }

        override fun onNavigationBlocked(webView: StripeConnectSheetWebView, url: Uri, isUserGesture: Boolean) {
            if (isUserGesture && sheetWebViews.containsValue(webView)) {
                callbacks.openOutsideSheet(url)
            }
        }

        override fun onWindowClosed(webView: StripeConnectSheetWebView) {
            if (pendingWindows.remove(webView) != null) {
                destroyLater(webView)
                return
            }
            val sheetId = sheetWebViews.entries.firstOrNull { it.value == webView }?.key ?: return
            val sheet = findSheet(sheetId) ?: return
            when (sheet.state) {
                State.SHOWING -> {
                    updateSheet(sheet.copy(state = State.CLOSING))
                    callbacks.onSheetClosed(sheetId)
                }
                State.STAGED -> removeSheet(sheetId)
                State.CLOSING -> Unit
            }
        }

        override fun onShowFileChooser(
            webView: StripeConnectSheetWebView,
            filePathCallback: ValueCallback<Array<Uri>>,
            fileChooserParams: WebChromeClient.FileChooserParams,
        ): Boolean {
            return callbacks.onShowFileChooser(webView, filePathCallback, fileChooserParams)
        }

        override fun onPermissionRequest(webView: StripeConnectSheetWebView, request: PermissionRequest) {
            callbacks.onPermissionRequest(webView, request)
        }
    }

    private fun findSheet(sheetId: String): InlineSheet? = _sheetsFlow.value.firstOrNull { it.id == sheetId }

    private fun updateSheet(sheet: InlineSheet) {
        _sheetsFlow.update { sheets -> sheets.map { if (it.id == sheet.id) sheet else it } }
    }

    private fun removeSheet(sheetId: String) {
        sheetWebViews.remove(sheetId)?.let(::destroyLater)
        _sheetsFlow.update { sheets -> sheets.filterNot { it.id == sheetId } }
    }

    // Web views can't be destroyed from inside their own callbacks.
    private fun destroyLater(webView: WebView) {
        webView.stopLoading()
        (webView.parent as? ViewGroup)?.removeView(webView)
        mainHandler.post { webView.destroy() }
    }
}
