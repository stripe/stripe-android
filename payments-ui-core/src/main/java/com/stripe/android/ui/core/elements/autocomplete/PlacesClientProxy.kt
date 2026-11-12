package com.stripe.android.ui.core.elements.autocomplete

import android.content.Context
import androidx.annotation.RestrictTo
import androidx.annotation.VisibleForTesting
import com.stripe.android.BuildConfig
import com.stripe.android.model.Address
import com.stripe.android.payments.core.analytics.ErrorReporter
import com.stripe.android.ui.core.elements.autocomplete.model.FindAutocompletePredictionsResponse
import java.util.Locale

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
interface PlacesClientProxy {
    suspend fun findAutocompletePredictions(
        query: String?,
        country: String,
        limit: Int
    ): Result<FindAutocompletePredictionsResponse>

    suspend fun fetchPlace(
        placeId: String,
        locale: Locale,
    ): Result<Address>

    fun resetSession()

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    companion object {
        @Volatile
        @VisibleForTesting
        var override: PlacesClientProxy? = null

        fun create(
            context: Context,
            googlePlacesApiKey: String,
            errorReporter: ErrorReporter
        ): PlacesClientProxy {
            return override ?: UnsupportedPlacesClientProxy(errorReporter)
        }

        fun getPlacesPoweredByGoogleDrawable(
            isSystemDarkTheme: Boolean,
        ): Int? = null
    }
}

internal class UnsupportedPlacesClientProxy(val errorReporter: ErrorReporter) : PlacesClientProxy {
    override fun resetSession() = Unit

    override suspend fun findAutocompletePredictions(
        query: String?,
        country: String,
        limit: Int
    ): Result<FindAutocompletePredictionsResponse> {
        val exception = IllegalStateException(
            "Missing Google Places dependency, please add it to your apps build.gradle"
        )
        if (BuildConfig.DEBUG) {
            throw exception
        }
        errorReporter.report(ErrorReporter.UnexpectedErrorEvent.FIND_AUTOCOMPLETE_PREDICTIONS_WITHOUT_DEPENDENCY)
        return Result.failure(exception)
    }

    override suspend fun fetchPlace(placeId: String, locale: Locale): Result<Address> {
        val exception = IllegalStateException(
            "Missing Google Places dependency, please add it to your apps build.gradle"
        )
        if (BuildConfig.DEBUG) {
            throw exception
        }
        errorReporter.report(ErrorReporter.UnexpectedErrorEvent.FETCH_PLACE_WITHOUT_DEPENDENCY)
        return Result.failure(exception)
    }
}
