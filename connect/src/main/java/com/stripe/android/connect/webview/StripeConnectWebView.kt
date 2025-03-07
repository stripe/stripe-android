package com.stripe.android.connect.webview

import android.annotation.SuppressLint
import android.app.Activity
import android.app.Application
import android.content.Context
import android.content.Intent
import android.content.MutableContextWrapper
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.webkit.JavascriptInterface
import android.webkit.PermissionRequest
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.annotation.VisibleForTesting
import androidx.lifecycle.findViewTreeLifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.stripe.android.connect.PrivateBetaConnectSDK
import com.stripe.android.connect.appearance.Appearance
import com.stripe.android.connect.util.findActivity
import com.stripe.android.connect.webview.serialization.AccountSessionClaimedMessage
import com.stripe.android.connect.webview.serialization.ConnectInstanceJs
import com.stripe.android.connect.webview.serialization.ConnectJson
import com.stripe.android.connect.webview.serialization.OpenFinancialConnectionsMessage
import com.stripe.android.connect.webview.serialization.PageLoadMessage
import com.stripe.android.connect.webview.serialization.SecureWebViewMessage
import com.stripe.android.connect.webview.serialization.SetCollectMobileFinancialConnectionsResultPayloadJs
import com.stripe.android.connect.webview.serialization.SetterFunctionCalledMessage
import com.stripe.android.connect.webview.serialization.toJs
import com.stripe.android.core.Logger
import com.stripe.android.core.version.StripeSdkVersion
import com.stripe.android.financialconnections.FinancialConnectionsSheetResult
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.encodeToJsonElement
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.put

/**
 * WebView to display an embedded Stripe.js component.
 */
@SuppressLint("ViewConstructor")
@Suppress("TooManyFunctions")
@OptIn(PrivateBetaConnectSDK::class)
internal class StripeConnectWebView private constructor(
    private val mutableContext: MutableContextWrapper,
    @property:VisibleForTesting internal val delegate: Delegate,
    private val logger: Logger,
) : WebView(mutableContext) {

    constructor(
        application: Application,
        delegate: Delegate,
        logger: Logger,
    ) : this(
        mutableContext = MutableContextWrapper(application),
        delegate = delegate,
        logger = logger,
    )

    private val loggerTag = javaClass.simpleName

    @VisibleForTesting
    internal val stripeWebViewClient = StripeConnectWebViewClient()

    @VisibleForTesting
    internal val stripeWebChromeClient = StripeConnectWebChromeClient()

    private val webViewLifecycleScope get() = findViewTreeLifecycleOwner()?.lifecycleScope

    init {
        webViewClient = stripeWebViewClient
        webChromeClient = stripeWebChromeClient
        settings.apply {
            @SuppressLint("SetJavaScriptEnabled")
            javaScriptEnabled = true

            domStorageEnabled = true
            loadWithOverviewMode = true
            useWideViewPort = true
            userAgentString = "$userAgentString - stripe-android/${StripeSdkVersion.VERSION_NAME}"
        }

        setDownloadListener(StripeDownloadListener(context))
        addJavascriptInterface(StripeJsInterface(), ANDROID_JS_INTERFACE)
    }

    fun updateConnectInstance(appearance: Appearance) {
        val payload = ConnectInstanceJs(appearance = appearance.toJs())
        evaluateSdkJs(
            "updateConnectInstance",
            ConnectJson.encodeToJsonElement(payload).jsonObject
        )
    }

    fun setCollectMobileFinancialConnectionsResult(
        id: String,
        result: FinancialConnectionsSheetResult?
    ) {
        val payload = SetCollectMobileFinancialConnectionsResultPayloadJs.from(id, result)
        callSetterWithSerializableValue(
            setter = "setCollectMobileFinancialConnectionsResult",
            value = ConnectJson.encodeToJsonElement(payload).jsonObject
        )
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        // We need the Activity context for some UI to work, like web-triggered dialogs
        mutableContext.baseContext = requireNotNull(findActivity())
    }

    override fun onDetachedFromWindow() {
        mutableContext.baseContext = mutableContext.applicationContext
        super.onDetachedFromWindow()
    }

    interface Delegate {
        var propsJson: JsonObject?

        /**
         * Fetch the client secret from the consumer of the SDK.
         */
        suspend fun fetchClientSecret(): String?

        /**
         * Get the initial parameters for the Connect SDK instance.
         */
        fun getInitialParams(context: Context): ConnectInstanceJs

        /**
         * Callback to invoke when the page started loading.
         */
        fun onPageStarted(url: String)

        /**
         * Callback to invoke when the page finished loading.
         */
        fun onPageFinished(url: String)

        /**
         * Callback to invoke when the webview received a network error. If the error was an HTTP error,
         * [httpStatusCode] will be non-null.
         */
        fun onReceivedError(
            requestUrl: String,
            httpStatusCode: Int?,
            errorMessage: String?,
            isMainPageLoad: Boolean
        )

        /**
         * Callback to invoke when the webview begins loading a URL. Returns false if the URL
         * should be loaded in the webview, true otherwise. Returning true has the effect of blocking
         * the webview's navigation to the URL.
         */
        fun shouldOverrideUrlLoading(activity: Activity, url: Uri): Boolean

        /**
         * Callback to invoke upon receiving a permission request from the webview.
         * Calls [PermissionRequest.grant] if the user grants permission to the resources
         * requested in [PermissionRequest.getResources], calls [PermissionRequest.deny] otherwise.
         *
         * An example of where this is called is when the webview requests access to the camera.
         */
        suspend fun onPermissionRequest(activity: Activity, request: PermissionRequest)

        /**
         * Callback to invoke upon receiving a file chooser request from the webview.
         */
        suspend fun onChooseFile(
            activity: Activity,
            filePathCallback: ValueCallback<Array<Uri>>,
            requestIntent: Intent
        )

        /**
         * Callback to invoke upon receiving 'onSetterFunctionCalled' message.
         */
        fun onReceivedSetterFunctionCalled(message: SetterFunctionCalledMessage)

        /**
         * Callback to invoke upon receiving 'pageDidLoad' message.
         */
        fun onReceivedPageDidLoad(pageViewId: String)

        /**
         * Callback whenever the merchant ID changes
         */
        fun onMerchantIdChanged(merchantId: String)

        /**
         * Callback to invoke upon receiving 'openFinancialConnections' message.
         */
        suspend fun onOpenFinancialConnections(activity: Activity, message: OpenFinancialConnectionsMessage)

        /**
         * Callback to invoke upon failing to deserialize a web message.
         */
        fun onErrorDeserializingWebMessage(webFunctionName: String, error: Throwable)
    }

    @VisibleForTesting
    internal inner class StripeConnectWebViewClient : WebViewClient() {
        override fun onPageStarted(view: WebView, url: String, favicon: Bitmap?) {
            delegate.onPageStarted(url)
        }

        override fun onPageFinished(view: WebView, url: String) {
            delegate.onPageFinished(url)
        }

        override fun onReceivedHttpError(
            view: WebView,
            request: WebResourceRequest,
            errorResponse: WebResourceResponse
        ) {
            delegate.onReceivedError(
                requestUrl = request.url.toString(),
                httpStatusCode = errorResponse.statusCode,
                errorMessage = errorResponse.reasonPhrase,
                isMainPageLoad = request.isForMainFrame
            )
        }

        override fun onReceivedError(view: WebView, request: WebResourceRequest, error: WebResourceError) {
            // for some reason errorCode and description are only available in API 23+,
            // so we simply ignore the description for older devices
            val errorMessage = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                error.description.toString()
            } else {
                null
            }
            delegate.onReceivedError(
                requestUrl = request.url.toString(),
                httpStatusCode = null,
                errorMessage = errorMessage,
                isMainPageLoad = request.isForMainFrame
            )
        }

        override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
            val activity = view.findActivity()
                ?: return false
            return delegate.shouldOverrideUrlLoading(activity, request.url)
        }
    }

    /**
     * A [WebChromeClient] that provides additional functionality for Stripe Connect Embedded Component WebViews.
     */
    internal inner class StripeConnectWebChromeClient : WebChromeClient() {
        override fun onPermissionRequest(request: PermissionRequest) {
            val activity = findActivity()
                ?: return request.deny()
            webViewLifecycleScope
                ?.launch {
                    delegate.onPermissionRequest(activity, request)
                }
                ?: return request.deny()
        }

        override fun onPermissionRequestCanceled(request: PermissionRequest) {
            super.onPermissionRequestCanceled(request)

            // currently a no-op since we don't hold any state from the permission
            // request and delegate all the UI to the Android system, meaning
            // there's no way for us to cancel any permissions UI
        }

        override fun onShowFileChooser(
            webView: WebView,
            filePathCallback: ValueCallback<Array<Uri>>,
            fileChooserParams: FileChooserParams
        ): Boolean {
            val activity = findActivity()
                ?: return false
            val lifecycleScope = webViewLifecycleScope
                ?: return false

            lifecycleScope.launch {
                delegate.onChooseFile(
                    activity = activity,
                    filePathCallback = filePathCallback,
                    requestIntent = fileChooserParams.createIntent()
                )
            }
            return true
        }
    }

    private inner class StripeJsInterface {
        @JavascriptInterface
        fun debug(message: String) {
            logger.debug("($loggerTag) Debug log from JS: $message")
        }

        @JavascriptInterface
        fun fetchInitComponentProps(): String {
            logger.debug("($loggerTag) InitComponentProps fetched")
            return ConnectJson.encodeToString(delegate.propsJson ?: JsonObject(emptyMap()))
        }

        @JavascriptInterface
        fun log(message: String) {
            logger.debug("($loggerTag) Log from JS: $message")
        }

        @JavascriptInterface
        fun fetchInitParams(): String {
            val initialParams = delegate.getInitialParams(context)
            logger.debug("($loggerTag) InitParams fetched: ${initialParams.toDebugString()}")
            return ConnectJson.encodeToString(initialParams)
        }

        @JavascriptInterface
        fun onSetterFunctionCalled(message: String) {
            val parsed = tryDeserializeWebMessage<SetterFunctionCalledMessage>(
                webFunctionName = "onSetterFunctionCalled",
                message = message,
            ) ?: return
            logger.debug("($loggerTag) Setter function called: $parsed")

            delegate.onReceivedSetterFunctionCalled(parsed)
        }

        @JavascriptInterface
        fun openSecureWebView(message: String) {
            val secureWebViewData = tryDeserializeWebMessage<SecureWebViewMessage>(
                webFunctionName = "openSecureWebView",
                message = message,
            )
            logger.debug("($loggerTag) Open secure web view with data: $secureWebViewData")
        }

        @JavascriptInterface
        fun pageDidLoad(message: String) {
            val pageLoadMessage = tryDeserializeWebMessage<PageLoadMessage>(
                webFunctionName = "pageDidLoad",
                message = message,
            ) ?: return
            logger.debug("($loggerTag) Page did load: $pageLoadMessage")

            delegate.onReceivedPageDidLoad(pageLoadMessage.pageViewId)
        }

        @JavascriptInterface
        fun accountSessionClaimed(message: String) {
            val accountSessionClaimedMessage = tryDeserializeWebMessage<AccountSessionClaimedMessage>(
                webFunctionName = "accountSessionClaimed",
                message = message,
            ) ?: return
            logger.debug("($loggerTag) Account session claimed: $accountSessionClaimedMessage")

            delegate.onMerchantIdChanged(accountSessionClaimedMessage.merchantId)
        }

        @JavascriptInterface
        fun openFinancialConnections(message: String) {
            val activity = findActivity()
                ?: return

            val parsed = ConnectJson.decodeFromString<OpenFinancialConnectionsMessage>(message)
            logger.debug("($loggerTag) Open FinancialConnections: $parsed")

            webViewLifecycleScope?.launch {
                delegate.onOpenFinancialConnections(
                    activity = activity,
                    message = parsed
                )
            }
        }

        @JavascriptInterface
        fun fetchClientSecret(): String {
            return runBlocking {
                checkNotNull(delegate.fetchClientSecret())
            }
        }
    }

    private inline fun <reified T> tryDeserializeWebMessage(
        webFunctionName: String,
        message: String,
    ): T? {
        return try {
            ConnectJson.decodeFromString<T>(message)
        } catch (e: IllegalArgumentException) {
            delegate.onErrorDeserializingWebMessage(
                webFunctionName = webFunctionName,
                error = e,
            )
            null
        }
    }

    private fun WebView.callSetterWithSerializableValue(setter: String, value: JsonElement) {
        evaluateSdkJs(
            "callSetterWithSerializableValue",
            buildJsonObject {
                put("setter", setter)
                put("value", value)
            }
        )
    }

    private fun WebView.evaluateSdkJs(function: String, payload: JsonObject) {
        val command = "$ANDROID_JS_INTERFACE.$function($payload)"
        post {
            logger.debug("($loggerTag) Evaluating JS: $command")
            evaluateJavascript(command, null)
        }
    }

    internal companion object {
        private const val ANDROID_JS_INTERFACE = "Android"
    }
}
