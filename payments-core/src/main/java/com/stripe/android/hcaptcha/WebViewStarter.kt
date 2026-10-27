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

internal fun interface WebViewStarter {
    suspend fun awaitStartUp(context: Context)
}

internal object DefaultWebViewStarter : WebViewStarter {
    private val backgroundExecutor = Dispatchers.IO.asExecutor()

    @OptIn(markerClass = [WebViewCompat.ExperimentalAsyncStartUp::class])
    override suspend fun awaitStartUp(context: Context) {
        suspendCancellableCoroutine { continuation ->
            val executor = Executor { command ->
                backgroundExecutor.execute {
                    runCatching {
                        command.run()
                    }.onFailure { exception ->
                        continuation.resumeWithException(exception)
                    }
                }
            }

            WebViewCompat.startUpWebView(
                context.applicationContext,
                WebViewStartUpConfig.Builder(executor).build()
            ) {
                continuation.resume(Unit)
            }
        }
    }
}
