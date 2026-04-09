package com.stripe.android.testing

import android.graphics.Bitmap
import app.cash.turbine.Turbine
import com.stripe.android.uicore.image.StripeImageLoader

internal class FakeStripeImageLoader(
    private val loadResult: Result<Bitmap?> = Result.success(null),
    private val loadResultByUrl: Map<String, Result<Bitmap?>> = emptyMap(),
    private val getResult: Result<Bitmap?> = Result.success(null),
) : StripeImageLoader {

    private val loadCalls = Turbine<LoadCall>()
    private val getCalls = Turbine<String>()

    override suspend fun load(url: String, width: Int, height: Int): Result<Bitmap?> {
        loadCalls.add(LoadCall(url = url, width = width, height = height))
        return loadResultByUrl[url] ?: loadResult
    }

    override suspend fun load(url: String): Result<Bitmap?> {
        loadCalls.add(LoadCall(url = url))
        return loadResultByUrl[url] ?: loadResult
    }

    override suspend fun get(url: String): Result<Bitmap?> {
        getCalls.add(url)
        return getResult
    }

    suspend fun awaitLoadCall(): LoadCall {
        return loadCalls.awaitItem()
    }

    suspend fun awaitGetCall(): String {
        return getCalls.awaitItem()
    }

    fun ensureAllEventsConsumed() {
        loadCalls.ensureAllEventsConsumed()
        getCalls.ensureAllEventsConsumed()
    }

    data class LoadCall(
        val url: String,
        val width: Int? = null,
        val height: Int? = null,
    )
}
