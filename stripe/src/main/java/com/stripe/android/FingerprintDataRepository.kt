package com.stripe.android

import android.content.Context
import android.os.Handler
import android.os.Looper
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import java.util.Calendar

internal interface FingerprintDataRepository {
    fun get(): LiveData<FingerprintData?>
    fun save(fingerprintData: FingerprintData)

    class Default(
        private val store: FingerprintDataStore,
        private val fingerprintRequestFactory: FingerprintRequestFactory,
        private val fingerprintRequestExecutor: FingerprintRequestExecutor =
            FingerprintRequestExecutor.Default(),
        private val handler: Handler = Handler(Looper.getMainLooper())
    ) : FingerprintDataRepository {
        private val timestampSupplier: () -> Long = {
            Calendar.getInstance().timeInMillis
        }

        constructor(
            context: Context,
            handler: Handler = Handler(Looper.getMainLooper())
        ) : this(
            store = FingerprintDataStore.Default(context),
            fingerprintRequestFactory = FingerprintRequestFactory(context),
            handler = handler
        )

        override fun get(): LiveData<FingerprintData?> {
            val resultData = MutableLiveData<FingerprintData?>()

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
                                resultData.value = remoteFingerprintData?.also {
                                    save(it)
                                }
                                liveData.removeObserver(this)
                            }
                        } else {
                            resultData.value = localFingerprintData
                            liveData.removeObserver(this)
                        }
                    }
                })
            }

            return resultData
        }

        override fun save(fingerprintData: FingerprintData) {
            store.save(fingerprintData)
        }
    }
}
