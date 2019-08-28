package com.stripe.example.service

import android.support.annotation.Size
import com.stripe.android.EphemeralKeyProvider
import com.stripe.android.EphemeralKeyUpdateListener
import com.stripe.example.module.RetrofitFactory
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import java.io.IOException

/**
 * An implementation of [EphemeralKeyProvider] that can be used to generate
 * ephemeral keys on the backend.
 */
class ExampleEphemeralKeyProvider(
    private val progressListener: ProgressListener
) : EphemeralKeyProvider {

    private val mCompositeDisposable: CompositeDisposable = CompositeDisposable()
    private val mStripeService: StripeService =
        RetrofitFactory.instance.create(StripeService::class.java)

    override fun createEphemeralKey(
        @Size(min = 4) apiVersion: String,
        keyUpdateListener: EphemeralKeyUpdateListener
    ) {
        val apiParamMap = hashMapOf("api_version" to apiVersion)

        mCompositeDisposable.add(mStripeService.createEphemeralKey(apiParamMap)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(
                { responseBody ->
                    try {
                        val rawKey = responseBody.string()
                        keyUpdateListener.onKeyUpdate(rawKey)
                        progressListener.onStringResponse(rawKey)
                    } catch (e: IOException) {
                        keyUpdateListener.onKeyUpdateFailure(0, e.message ?: "")
                    }
                },
                { throwable -> progressListener.onStringResponse(throwable.message ?: "") }))
    }

    interface ProgressListener {
        fun onStringResponse(response: String)
    }
}
