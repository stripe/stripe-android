package com.stripe.example.activity

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.stripe.android.createSource
import com.stripe.android.model.Source
import com.stripe.android.model.SourceParams
import com.stripe.android.retrieveSource
import com.stripe.example.StripeFactory
import kotlinx.coroutines.launch

internal class SourceViewModel(
    application: Application
) : AndroidViewModel(application) {
    private val stripe = StripeFactory(application).create()

    internal var source: Source? = null

    internal fun createSource(sourceParams: SourceParams): LiveData<Result<Source>> {
        val resultData = MutableLiveData<Result<Source>>()
        viewModelScope.launch {
            resultData.value = runCatching {
                stripe.createSource(sourceParams)
            }
        }
        return resultData
    }

    internal fun fetchSource(source: Source?): LiveData<Result<Source>> {
        val resultData = MutableLiveData<Result<Source>>()
        if (source != null) {
            viewModelScope.launch {
                resultData.value = runCatching {
                    stripe.retrieveSource(
                        source.id.orEmpty(),
                        source.clientSecret.orEmpty()
                    )
                }
            }
        } else {
            resultData.value = Result.failure(
                IllegalArgumentException("Create and authenticate a Source before fetching it.")
            )
        }
        return resultData
    }
}
