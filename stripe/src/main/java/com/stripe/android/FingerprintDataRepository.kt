package com.stripe.android

import android.content.Context
import android.os.Handler
import android.os.Looper
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
            FingerprintRequestExecutor.Default(),
        private val handler: Handler = Handler(Looper.getMainLooper())
    ) : FingerprintDataRepository {
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
                liveData.observeForever(object : Observer<FingerprintData?> {
                    override fun onChanged(localFingerprintData: FingerprintData?) {
                        if (localFingerprintData != null) {
                            // FingerprintData was available locally
                            resultData.value = localFingerprintData
                            liveData.removeObserver(this)
                        } else {
                            // FingerprintData needs to be fetched remotely
                            fingerprintRequestExecutor.execute(
                                // TODO(mshafrir-stripe): pass in fingerprint GUID
                                request = fingerprintRequestFactory.create(null)
                            ) { remoteFingerprintData ->
                                resultData.value = remoteFingerprintData?.also {
                                    save(it)
                                }
                                liveData.removeObserver(this)
                            }
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
