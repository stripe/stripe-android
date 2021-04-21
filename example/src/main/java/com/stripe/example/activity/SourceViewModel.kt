package com.stripe.example.activity

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.liveData
import com.stripe.android.createSource
import com.stripe.android.model.Source
import com.stripe.android.model.SourceParams
import com.stripe.android.retrieveSource
import com.stripe.example.StripeFactory

internal class SourceViewModel(
    application: Application
) : AndroidViewModel(application) {
    private val stripe = StripeFactory(application).create()

    internal var source: Source? = null

    internal fun createSource(sourceParams: SourceParams) = liveData {
        emit(
            runCatching {
                stripe.createSource(sourceParams)
            }
        )
    }

    internal fun fetchSource(source: Source?) = liveData {
        if (source == null) {
            emit(
                Result.failure<Source>(
                    IllegalArgumentException("Create and authenticate a Source before fetching it.")
                )
            )
        } else {
            emit(
                runCatching {
                    stripe.retrieveSource(
                        source.id.orEmpty(),
                        source.clientSecret.orEmpty()
                    )
                }
            )
        }
    }
}
