package com.stripe.example.activity

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
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

    internal var source: Source? = null

    internal fun createSource(sourceParams: SourceParams): LiveData<SourceResult> {
        val resultData = MutableLiveData<SourceResult>()
        stripe.createSource(
            sourceParams = sourceParams,
            callback = object : ApiResultCallback<Source> {
                override fun onSuccess(result: Source) {
                    resultData.value = SourceResult.Success(result)
                }

                override fun onError(e: Exception) {
                    resultData.value = SourceResult.Error(e)
                }
            })
        return resultData
    }

    internal fun fetchSource(source: Source?): LiveData<SourceResult> {
        val resultData = MutableLiveData<SourceResult>()
        if (source != null) {
            stripe.retrieveSource(
                source.id.orEmpty(),
                source.clientSecret.orEmpty(),
                callback = object : ApiResultCallback<Source> {
                    override fun onSuccess(result: Source) {
                        resultData.value = SourceResult.Success(result)
                    }

                    override fun onError(e: Exception) {
                        resultData.value = SourceResult.Error(e)
                    }
                }
            )
        } else {
            resultData.value = SourceResult.Error(
                IllegalArgumentException("Create and authenticate a Source before fetching it.")
            )
        }
        return resultData
    }

    internal sealed class SourceResult {
        data class Success(val source: Source) : SourceResult()
        data class Error(val e: Exception) : SourceResult()
    }
}
