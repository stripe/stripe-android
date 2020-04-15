package com.stripe.android

import android.content.Context
import android.os.Handler
import android.os.Looper
import androidx.lifecycle.Observer
import java.util.Calendar

internal interface FingerprintDataRepository {
    fun refresh()
    fun get(): FingerprintData?
    fun save(fingerprintData: FingerprintData)

    class Default(
        private val store: FingerprintDataStore,
        private val fingerprintRequestFactory: FingerprintRequestFactory,
        private val fingerprintRequestExecutor: FingerprintRequestExecutor =
            FingerprintRequestExecutor.Default(),
        private val handler: Handler = Handler(Looper.getMainLooper())
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
            handler.post {
                val liveData = store.get()
                // LiveData observation must occur on the main thread
                liveData.observeForever(object : Observer<FingerprintData> {
                    override fun onChanged(localFingerprintData: FingerprintData) {
                        if (localFingerprintData.isExpired(timestampSupplier())) {
                            fingerprintRequestExecutor.execute(
                                request = fingerprintRequestFactory.create(
                                    localFingerprintData.guid
                                )
                            ) { remoteFingerprintData ->
                                remoteFingerprintData?.let {
                                    save(it)
                                }
                                liveData.removeObserver(this)
                            }
                        } else {
                            cachedFingerprintData = localFingerprintData
                            liveData.removeObserver(this)
                        }
                    }
                })
            }
        }

        override fun get(): FingerprintData? {
            return cachedFingerprintData
        }

        override fun save(fingerprintData: FingerprintData) {
            cachedFingerprintData = fingerprintData
            store.save(fingerprintData)
        }
    }
}
