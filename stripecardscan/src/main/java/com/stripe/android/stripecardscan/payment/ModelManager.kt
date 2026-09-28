package com.stripe.android.stripecardscan.payment

import android.content.Context
import androidx.annotation.VisibleForTesting
import com.stripe.android.stripecardscan.framework.FetchedData
import com.stripe.android.stripecardscan.framework.Fetcher
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

internal abstract class ModelManager {
    private lateinit var fetcher: Fetcher
    private val fetcherMutex = Mutex()

    private var onFetch: ((success: Boolean) -> Unit)? = null

    suspend fun fetchModel(
        context: Context,
        forImmediateUse: Boolean,
        isOptional: Boolean = false
    ): FetchedData {
        fetcherMutex.withLock {
            if (!this::fetcher.isInitialized) {
                fetcher = getModelFetcher(context.applicationContext)
            }
        }
        return fetcher.fetchData(forImmediateUse, isOptional).also {
            onFetch?.invoke(it.successfullyFetched)
        }
    }

    suspend fun isReady() = fetcherMutex.withLock {
        if (this::fetcher.isInitialized) fetcher.isCached() else false
    }

    @VisibleForTesting(otherwise = VisibleForTesting.PROTECTED)
    abstract fun getModelFetcher(context: Context): Fetcher
}
