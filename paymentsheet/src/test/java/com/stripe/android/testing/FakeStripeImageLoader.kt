package com.stripe.android.testing

import android.graphics.Bitmap
import app.cash.turbine.Turbine
import com.stripe.android.uicore.image.StripeImageLoader

internal class FakeStripeImageLoader(
    private val loadResult: Result<Bitmap?> = Result.success(null),
    private val loadResultByUrl: Map<String, Result<Bitmap?>> = emptyMap(),
) : StripeImageLoader {

    private val loadCalls = Turbine<LoadCall>()

    override suspend fun load(url: String, width: Int, height: Int): Result<Bitmap?> {
        loadCalls.add(LoadCall(url = url, width = width, height = height))
        return loadResultByUrl[url] ?: loadResult
    }

    override suspend fun load(url: String): Result<Bitmap?> {
        loadCalls.add(LoadCall(url = url))
        return loadResultByUrl[url] ?: loadResult
    }

    suspend fun awaitLoadCall(): LoadCall {
        return loadCalls.awaitItem()
    }

    fun ensureAllEventsConsumed() {
        loadCalls.ensureAllEventsConsumed()
    }

    data class LoadCall(
        val url: String,
        val width: Int? = null,
        val height: Int? = null,
    )
}
