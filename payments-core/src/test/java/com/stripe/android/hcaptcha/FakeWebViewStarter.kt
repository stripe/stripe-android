package com.stripe.android.hcaptcha

import android.content.Context
import app.cash.turbine.Turbine
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

internal class FakeWebViewStarter(
    private val completeImmediately: Boolean = true
) : WebViewStarter {
    private val calls = Turbine<Call>()

    override suspend fun awaitStartUp(context: Context) {
        suspendCancellableCoroutine<Unit> { continuation ->
            calls.add(Call(context, continuation))
            if (completeImmediately) {
                continuation.resume(Unit)
            }
        }
    }

    suspend fun awaitCall(): Call = calls.awaitItem()

    fun ensureAllEventsConsumed() = calls.ensureAllEventsConsumed()

    data class Call(
        val context: Context,
        val continuation: CancellableContinuation<Unit>
    )
}
