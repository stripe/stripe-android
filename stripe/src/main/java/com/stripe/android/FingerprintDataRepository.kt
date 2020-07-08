package com.stripe.android

import android.content.Context
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.switchMap
import java.util.Calendar
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
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
        private val coroutineScope: CoroutineScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    ) : FingerprintDataRepository {
        private var cachedFingerprintData: FingerprintData? = null

        private val timestampSupplier: () -> Long = {
            Calendar.getInstance().timeInMillis
        }

        constructor(
            context: Context
        ) : this(
            store = FingerprintDataStore.Default(context),
            fingerprintRequestFactory = FingerprintRequestFactory(context)
        )

        override fun refresh() {
            if (Stripe.advancedFraudSignalsEnabled) {
                coroutineScope.launch {
                    store.get().switchMap { localFingerprintData ->
                        if (localFingerprintData == null ||
                            localFingerprintData.isExpired(timestampSupplier())) {
                            fingerprintRequestExecutor.execute(
                                request = fingerprintRequestFactory.create(
                                    localFingerprintData
                                )
                            )
                        } else {
                            MutableLiveData(localFingerprintData)
                        }
                    }.let { liveData ->
                        liveData.observeForever(object : Observer<FingerprintData?> {
                            override fun onChanged(fingerprintData: FingerprintData?) {
                                if (cachedFingerprintData != fingerprintData) {
                                    fingerprintData?.let {
                                        save(it)
                                    }
                                }
                                liveData.removeObserver(this)
                            }
                        })
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
