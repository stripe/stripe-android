import android.graphics.Bitmap
import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.webkit.WebViewClient
import com.stripe.android.connectsdk.EmbeddedComponentManager
import com.stripe.android.connectsdk.FetchClientSecretCallback
import com.stripe.android.connectsdk.PrivateBetaConnectSDK
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@OptIn(PrivateBetaConnectSDK::class)
internal class StripeConnectWebViewClient: WebViewClient() {

    override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
        initJavascriptBridge(view!!)
    }

    private fun initJavascriptBridge(webView: WebView) {
        webView.evaluateJavascript(
            """
                Android.accountSessionClaimed = (message) => {
                    AndroidInternal.accountSessionClaimed(JSON.stringify(message));
                };
                
                Android.pageDidLoad = (message) => {
                    AndroidInternal.pageDidLoad(JSON.stringify(message));
                };

                Android.fetchClientSecret = () => {
                    return new Promise((resolve, reject) => {
                        try {
                            resolve(AndroidInternal.fetchClientSecret());
                        } catch (e) {
                            reject(e);
                        }
                    });
                };

                Android.onSetterFunctionCalled = (message) => {
                    AndroidInternal.onSetterFunctionCalled(JSON.stringify(message));
                };

                Android.openSecureWebView = (message) => {
                    AndroidInternal.openSecureWebView(JSON.stringify(message));
                };
            """.trimIndent(),
            null
        )
    }

    inner class WebLoginJsInterface {
        @JavascriptInterface
        fun debug(message: String) {
            println("Debug log from JS: $message")
        }

        @JavascriptInterface
        fun fetchInitParams() {
            println("InitParams fetched")
        }
    }

    inner class WebLoginJsInterfaceInternal {
        @JavascriptInterface
        fun log(message: String) {
            println("Log from JS: $message")
        }

        @JavascriptInterface
        fun onSetterFunctionCalled(message: String) {
            val setterMessage = Json.decodeFromString<SetterMessage>(message)
            println("Setter function called: $setterMessage")
        }

        @JavascriptInterface
        fun openSecureWebView(message: String) {
            val secureWebViewData = Json.decodeFromString<SecureWebViewMessage>(message)
            println("Open secure web view with data: $secureWebViewData")
        }

        @JavascriptInterface
        fun pageDidLoad(message: String) {
            val pageLoadMessage = Json.decodeFromString<PageLoadMessage>(message)
            println("Page did load: $pageLoadMessage")
        }

        @JavascriptInterface
        fun accountSessionClaimed(message: String) {
            val accountSessionClaimedMessage = Json.decodeFromString<AccountSessionClaimedMessage>(message)
            println("Account session claimed: $accountSessionClaimedMessage")
        }

        @JavascriptInterface
        fun fetchClientSecret(): String {
            return runBlocking {
                suspendCancellableCoroutine { continuation ->
                    EmbeddedComponentManager.instance!!.fetchClientSecret.fetchClientSecret(
                        object : FetchClientSecretCallback.ClientSecretResultCallback {
                            override fun onResult(secret: String) {
                                continuation.resume(secret)
                            }

                            override fun onError() {
                                continuation.resumeWithException(Exception("Failed to fetch client secret"))
                            }
                        }
                    )
                }
            }
        }
    }

    @Serializable
    data class AccountSessionClaimedMessage(
        val merchantId: String
    )

    @Serializable
    data class PageLoadMessage(
        val pageViewId: String
    )

    @Serializable
    data class SetterMessage(
        val setter: String,
        val value: String,
    )

    @Serializable
    data class SecureWebViewMessage(
        val id: String,
        val url: String
    )
}
