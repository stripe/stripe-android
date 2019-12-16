package com.stripe.example.activity

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import com.stripe.android.ApiResultCallback
import com.stripe.android.Stripe
import com.stripe.android.model.Source
import com.stripe.android.model.SourceParams
import com.stripe.example.Settings

internal class SourceViewModel(
    application: Application
) : AndroidViewModel(application) {
    private val stripe = Stripe(
        application.applicationContext,
        Settings(application.applicationContext).publishableKey
    )

    @JvmSynthetic
    internal val createdSource: MutableLiveData<Source> = MutableLiveData()

    @JvmSynthetic
    internal val createdSourceException: MutableLiveData<Exception> = MutableLiveData()

    internal fun createSource(sourceParams: SourceParams) {
        stripe.createSource(
            sourceParams = sourceParams,
            callback = object : ApiResultCallback<Source> {
                override fun onSuccess(result: Source) {
                    createdSource.value = result
                }

                override fun onError(e: Exception) {
                    createdSourceException.value = e
                }
            })
    }
}
