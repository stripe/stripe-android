package com.stripe.example.service

import androidx.annotation.Size
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
class ExampleEphemeralKeyProvider : EphemeralKeyProvider {

    private val compositeDisposable: CompositeDisposable = CompositeDisposable()
    private val backendApi: BackendApi =
            RetrofitFactory.instance.create(BackendApi::class.java)

    override fun createEphemeralKey(
        @Size(min = 4) apiVersion: String,
        keyUpdateListener: EphemeralKeyUpdateListener
    ) {
        compositeDisposable.add(
            backendApi.createEphemeralKey(hashMapOf("api_version" to apiVersion))
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe { responseBody ->
                    try {
                        val ephemeralKeyJson = responseBody.string()
                        keyUpdateListener.onKeyUpdate(ephemeralKeyJson)
                    } catch (e: IOException) {
                        keyUpdateListener
                            .onKeyUpdateFailure(0, e.message ?: "")
                    }
                })
    }
}
