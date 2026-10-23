package com.stripe.android.hcaptcha

import android.content.Context
import androidx.annotation.OptIn
import androidx.webkit.WebViewCompat
import androidx.webkit.WebViewStartUpConfig
import app.cash.turbine.Turbine
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.async
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.mockito.Mockito.mockStatic
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import kotlin.test.Test

@OptIn(markerClass = [WebViewCompat.ExperimentalAsyncStartUp::class])
internal class DefaultWebViewStarterTest {
    @Test
    fun `startup waits for success callback`() = runScenario {
        val startup = scope.async { DefaultWebViewStarter.awaitStartUp(context) }
        scope.runCurrent()

        val call = startUpCalls.awaitItem()
        assertThat(call.context).isSameInstanceAs(applicationContext)
        assertThat(call.config.shouldRunUiThreadStartUpTasks()).isTrue()
        assertThat(startup.isCompleted).isFalse()

        call.callback.onSuccess(mock())

        startup.await()
        assertThat(startup.isCompleted).isTrue()
    }

    @Test
    fun `startup failure reaches the caller`() {
        val exception = IllegalStateException("WebView startup unavailable")

        runScenario(startUpException = exception) {
            val result = runCatching { DefaultWebViewStarter.awaitStartUp(context) }

            startUpCalls.awaitItem()
            assertThat(result.exceptionOrNull()).isSameInstanceAs(exception)
        }
    }

    @Test
    fun `cancellation completes without waiting for startup and ignores a late callback`() = runScenario {
        val startup = scope.async { DefaultWebViewStarter.awaitStartUp(context) }
        scope.runCurrent()

        val call = startUpCalls.awaitItem()
        assertThat(startup.isCompleted).isFalse()

        startup.cancelAndJoin()

        assertThat(startup.isCancelled).isTrue()
        assertThat(startup.isCompleted).isTrue()

        call.callback.onSuccess(mock())

        scope.runCurrent()
        assertThat(startup.isCancelled).isTrue()
    }

    private fun runScenario(
        startUpException: Throwable? = null,
        block: suspend Scenario.() -> Unit
    ) = runTest {
        val applicationContext = mock<Context>()
        val context = mock<Context> {
            on { this.applicationContext } doReturn applicationContext
        }
        val startUpCalls = Turbine<StartUpCall>()

        mockStatic(WebViewCompat::class.java).use { webViewCompat ->
            webViewCompat.`when`<Unit> {
                WebViewCompat.startUpWebView(any(), any(), any())
            }.thenAnswer { invocation ->
                startUpCalls.add(
                    StartUpCall(
                        context = invocation.getArgument(0),
                        config = invocation.getArgument(1),
                        callback = invocation.getArgument(2)
                    )
                )
                startUpException?.let { throw it }
                null
            }

            Scenario(
                scope = this,
                context = context,
                applicationContext = applicationContext,
                startUpCalls = startUpCalls
            ).block()

            startUpCalls.ensureAllEventsConsumed()
        }
    }

    private data class Scenario(
        val scope: TestScope,
        val context: Context,
        val applicationContext: Context,
        val startUpCalls: Turbine<StartUpCall>
    )

    private data class StartUpCall(
        val context: Context,
        val config: WebViewStartUpConfig,
        val callback: WebViewCompat.WebViewStartUpCallback
    )
}
