package com.stripe.samplestore.service

import androidx.annotation.Size
import com.stripe.android.EphemeralKeyProvider
import com.stripe.android.EphemeralKeyUpdateListener
import com.stripe.samplestore.RetrofitFactory
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import java.io.IOException

class SampleStoreEphemeralKeyProvider @JvmOverloads constructor(
    private val stripeAccountId: String? = null
) : EphemeralKeyProvider {

    private val compositeDisposable: CompositeDisposable = CompositeDisposable()
    private val backendApi: BackendApi =
        RetrofitFactory.instance.create(BackendApi::class.java)

    override fun createEphemeralKey(
        @Size(min = 4) apiVersion: String,
        keyUpdateListener: EphemeralKeyUpdateListener
    ) {
        val params = hashMapOf("api_version" to apiVersion)
        if (stripeAccountId != null) {
            params["stripe_account"] = stripeAccountId
        }

        compositeDisposable.add(backendApi.createEphemeralKey(params)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe { response ->
                try {
                    val rawKey = response.string()
                    keyUpdateListener.onKeyUpdate(rawKey)
                } catch (e: IOException) {
                    keyUpdateListener
                        .onKeyUpdateFailure(0, e.message ?: "")
                }
            })
    }

    fun destroy() {
        compositeDisposable.dispose()
    }
}
