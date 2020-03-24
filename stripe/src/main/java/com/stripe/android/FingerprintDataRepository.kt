package com.stripe.android

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer

internal interface FingerprintDataRepository {
    fun get(): LiveData<FingerprintData?>
    fun save(fingerprintData: FingerprintData)

    class Default(
        private val store: FingerprintDataStore,
        private val fingerprintRequestFactory: FingerprintRequestFactory,
        private val fingerprintRequestExecutor: FingerprintRequestExecutor =
            FingerprintRequestExecutor.Default()
    ) : FingerprintDataRepository {

        constructor(context: Context) : this(
            store = FingerprintDataStore.Default(context),
            fingerprintRequestFactory = FingerprintRequestFactory(context)
        )

        override fun get(): LiveData<FingerprintData?> {
            val resultData = MutableLiveData<FingerprintData?>()

            val storeData = store.get()
            storeData.observeForever(object : Observer<FingerprintData?> {
                override fun onChanged(localFingerprintData: FingerprintData?) {
                    if (localFingerprintData != null) {
                        // FingerprintData was available locally
                        resultData.value = localFingerprintData
                        storeData.removeObserver(this)
                    } else {
                        // FingerprintData needs to be fetched remotely
                        fingerprintRequestExecutor.execute(
                            request = fingerprintRequestFactory.create()
                        ) { remoteFingerprintData ->
                            resultData.value = remoteFingerprintData?.also {
                                save(it)
                            }
                            storeData.removeObserver(this)
                        }
                    }
                }
            })

            return resultData
        }

        override fun save(fingerprintData: FingerprintData) {
            store.save(fingerprintData)
        }
    }
}
