package com.stripe.android

import android.content.Context
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Calendar

internal interface FingerprintDataRepository {
    fun refresh()
    fun get(): FingerprintData?
    fun save(fingerprintData: FingerprintData)

    class Default(
        private val localStore: FingerprintDataStore,
        private val fingerprintRequestFactory: FingerprintRequestFactory,
        private val fingerprintRequestExecutor: FingerprintRequestExecutor =
            FingerprintRequestExecutor.Default(),
        dispatcher: CoroutineDispatcher = Dispatchers.IO
    ) : FingerprintDataRepository {
        private var cachedFingerprintData: FingerprintData? = null

        private val timestampSupplier: () -> Long = {
            Calendar.getInstance().timeInMillis
        }

        private val scope = CoroutineScope(dispatcher)

        constructor(
            context: Context
        ) : this(
            localStore = FingerprintDataStore.Default(context),
            fingerprintRequestFactory = FingerprintRequestFactory(context)
        )

        override fun refresh() {
            if (Stripe.advancedFraudSignalsEnabled) {
                scope.launch {
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
