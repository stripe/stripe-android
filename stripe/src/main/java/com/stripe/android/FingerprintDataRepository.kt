package com.stripe.android

import android.content.Context
import java.util.Calendar
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flatMapMerge
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch

internal interface FingerprintDataRepository {
    fun refresh()
    fun get(): FingerprintData?
    fun save(fingerprintData: FingerprintData)

    class Default(
        private val store: FingerprintDataStore,
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
            store = FingerprintDataStore.Default(context),
            fingerprintRequestFactory = FingerprintRequestFactory(context)
        )

        @OptIn(FlowPreview::class)
        override fun refresh() {
            if (Stripe.advancedFraudSignalsEnabled) {
                scope.launch {
                    store.get().flatMapMerge { localFingerprintData ->
                        if (localFingerprintData == null ||
                            localFingerprintData.isExpired(timestampSupplier())) {
                            fingerprintRequestExecutor.execute(
                                request = fingerprintRequestFactory.create(
                                    localFingerprintData
                                )
                            )
                        } else {
                            flowOf(localFingerprintData)
                        }
                    }.collect { fingerprintData ->
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
            store.save(fingerprintData)
        }
    }
}
