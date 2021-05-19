package com.stripe.android

import android.content.Context
import androidx.annotation.UiThread
import androidx.annotation.WorkerThread
import com.stripe.android.networking.FingerprintRequestExecutor
import com.stripe.android.networking.FingerprintRequestFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Calendar
import kotlin.coroutines.CoroutineContext

internal interface FingerprintDataRepository {
    @UiThread
    fun refresh()

    /**
     * Get the cached [FingerprintData]. This is not a blocking request.
     */
    @UiThread
    fun getCached(): FingerprintData?

    /**
     * Get the latest [FingerprintData]. This is a blocking request.
     *
     * 1. From [FingerprintDataStore] if that value is not expired.
     * 2. Otherwise, from the network.
     */
    @WorkerThread
    suspend fun getLatest(): FingerprintData?

    @UiThread
    fun save(fingerprintData: FingerprintData)

    class Default(
        private val localStore: FingerprintDataStore,
        private val fingerprintRequestFactory: FingerprintRequestFactory,
        private val fingerprintRequestExecutor: FingerprintRequestExecutor =
            FingerprintRequestExecutor.Default(),
        private val workContext: CoroutineContext
    ) : FingerprintDataRepository {
        private var cachedFingerprintData: FingerprintData? = null

        private val timestampSupplier: () -> Long = {
            Calendar.getInstance().timeInMillis
        }

        @JvmOverloads
        constructor(
            context: Context,
            workContext: CoroutineContext = Dispatchers.IO
        ) : this(
            localStore = FingerprintDataStore.Default(context, workContext),
            fingerprintRequestFactory = FingerprintRequestFactory.Default(context),
            workContext = workContext
        )

        override fun refresh() {
            if (Stripe.advancedFraudSignalsEnabled) {
                CoroutineScope(workContext).launch {
                    getLatest()
                }
            }
        }

        override suspend fun getLatest() = withContext(workContext) {
            val latestFingerprintData = localStore.get().let { localFingerprintData ->
                if (localFingerprintData == null ||
                    localFingerprintData.isExpired(timestampSupplier())
                ) {
                    fingerprintRequestExecutor.execute(
                        request = fingerprintRequestFactory.create(
                            localFingerprintData
                        )
                    )
                } else {
                    localFingerprintData
                }
            }

            if (cachedFingerprintData != latestFingerprintData) {
                latestFingerprintData?.let(::save)
            }

            latestFingerprintData
        }

        override fun getCached(): FingerprintData? {
            return cachedFingerprintData.takeIf {
                Stripe.advancedFraudSignalsEnabled
            }
        }

        override fun save(fingerprintData: FingerprintData) {
            cachedFingerprintData = fingerprintData
            localStore.save(fingerprintData)
        }
    }
}
