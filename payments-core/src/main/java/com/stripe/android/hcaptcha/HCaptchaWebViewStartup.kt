package com.stripe.android.hcaptcha

import android.content.Context
import androidx.annotation.OptIn
import androidx.webkit.WebViewCompat
import androidx.webkit.WebViewStartUpConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.asExecutor
import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.concurrent.Executor
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

internal object HCaptchaWebViewStartup {
    @OptIn(WebViewCompat.ExperimentalAsyncStartUp::class)
    @Suppress("TooGenericExceptionCaught") // Forward failures from the provider's startup Runnable.
    suspend fun awaitReady(context: Context) = suspendCancellableCoroutine { continuation ->
        // WebKit 1.15's startup callback has no failure method. Forward failures from
        // its executor to the caller so missing/broken WebView providers don't crash
        // a worker thread or leave CAPTCHA confirmation waiting for startup forever.
        val executor = Executor { command ->
            Dispatchers.IO.asExecutor().execute {
                try {
                    command.run()
                } catch (error: Exception) {
                    if (continuation.isActive) {
                        continuation.resumeWithException(error)
                    }
                }
            }
        }
        WebViewCompat.startUpWebView(
            context,
            WebViewStartUpConfig.Builder(executor).build(),
        ) {
            if (continuation.isActive) {
                continuation.resume(Unit)
            }
        }
    }
}
