package com.stripe.android

import android.content.Context
import com.stripe.android.networking.FingerprintRequestExecutor
import com.stripe.android.networking.FingerprintRequestFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Calendar
import kotlin.coroutines.CoroutineContext

internal interface FingerprintDataRepository {
    fun refresh()
    fun get(): FingerprintData?
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

        constructor(
            context: Context
        ) : this(
            localStore = FingerprintDataStore.Default(context),
            fingerprintRequestFactory = FingerprintRequestFactory(context),
            workContext = Dispatchers.IO
        )

        override fun refresh() {
            if (Stripe.advancedFraudSignalsEnabled) {
                CoroutineScope(workContext).launch {
                    localStore.get().let { localFingerprintData ->
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
                    }.let { fingerprintData ->
                        if (cachedFingerprintData != fingerprintData) {
                            fingerprintData?.let {
                                save(it)
                            }
                        }
                    }
                }
            }
        }

        override fun get(): FingerprintData? {
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
