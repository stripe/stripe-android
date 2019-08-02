package com.stripe.samplestore.service

import android.support.annotation.Size
import com.stripe.android.EphemeralKeyProvider
import com.stripe.android.EphemeralKeyUpdateListener
import com.stripe.samplestore.RetrofitFactory
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import java.io.IOException
import java.util.HashMap

class SampleStoreEphemeralKeyProvider @JvmOverloads constructor(
    private val progressListener: ProgressListener,
    private val stripeAccountId: String? = null
) : EphemeralKeyProvider {

    private val compositeDisposable: CompositeDisposable = CompositeDisposable()
    private val stripeService: StripeService =
        RetrofitFactory.instance.create(StripeService::class.java)

    override fun createEphemeralKey(
        @Size(min = 4) apiVersion: String,
        keyUpdateListener: EphemeralKeyUpdateListener
    ) {
        val apiParamMap = HashMap<String, String>()
        apiParamMap["api_version"] = apiVersion
        if (stripeAccountId != null) {
            apiParamMap["stripe_account"] = stripeAccountId
        }

        compositeDisposable.add(stripeService.createEphemeralKey(apiParamMap)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(
                { response ->
                    try {
                        val rawKey = response.string()
                        keyUpdateListener.onKeyUpdate(rawKey)
                        progressListener.onStringResponse(rawKey)
                    } catch (e: IOException) {
                        keyUpdateListener
                            .onKeyUpdateFailure(0, e.message ?: "")
                    }
                },
                { throwable ->
                    progressListener
                        .onStringResponse(throwable.message ?: "")
                }))
    }

    fun destroy() {
        compositeDisposable.dispose()
    }

    interface ProgressListener {
        fun onStringResponse(string: String)
    }
}
