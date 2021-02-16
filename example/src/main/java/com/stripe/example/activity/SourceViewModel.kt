package com.stripe.example.activity

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.stripe.android.ApiResultCallback
import com.stripe.android.model.Source
import com.stripe.android.model.SourceParams
import com.stripe.example.StripeFactory

internal class SourceViewModel(
    application: Application
) : AndroidViewModel(application) {
    private val stripe = StripeFactory(application).create()

    internal var source: Source? = null

    internal fun createSource(sourceParams: SourceParams): LiveData<Result<Source>> {
        val resultData = MutableLiveData<Result<Source>>()
        stripe.createSource(
            sourceParams = sourceParams,
            callback = object : ApiResultCallback<Source> {
                override fun onSuccess(result: Source) {
                    resultData.value = Result.success(result)
                }

                override fun onError(e: Exception) {
                    resultData.value = Result.failure(e)
                }
            }
        )
        return resultData
    }

    internal fun fetchSource(source: Source?): LiveData<Result<Source>> {
        val resultData = MutableLiveData<Result<Source>>()
        if (source != null) {
            stripe.retrieveSource(
                source.id.orEmpty(),
                source.clientSecret.orEmpty(),
                callback = object : ApiResultCallback<Source> {
                    override fun onSuccess(result: Source) {
                        resultData.value = Result.success(result)
                    }

                    override fun onError(e: Exception) {
                        resultData.value = Result.failure(e)
                    }
                }
            )
        } else {
            resultData.value = Result.failure(
                IllegalArgumentException("Create and authenticate a Source before fetching it.")
            )
        }
        return resultData
    }
}
