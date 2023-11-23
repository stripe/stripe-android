package com.stripe.example.activity

import android.app.Application
import androidx.lifecycle.AndroidViewModel
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

    internal suspend fun createSource(sourceParams: SourceParams): Result<Source> {
        return runCatching {
            stripe.createSource(sourceParams)
        }
    }

    internal suspend fun fetchSource(source: Source?): Result<Source> {
        return if (source == null) {
            Result.failure(
                IllegalArgumentException("Create and authenticate a Source before fetching it.")
            )
        } else {
            runCatching {
                stripe.retrieveSource(
                    source.id.orEmpty(),
                    source.clientSecret.orEmpty()
                )
            }
        }
    }
}
