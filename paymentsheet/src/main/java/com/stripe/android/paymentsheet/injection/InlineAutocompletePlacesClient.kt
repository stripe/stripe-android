package com.stripe.android.paymentsheet.injection

import android.content.Context
import com.stripe.android.core.utils.FeatureFlags
import com.stripe.android.model.Address
import com.stripe.android.payments.core.analytics.ErrorReporter
import com.stripe.android.ui.core.elements.autocomplete.PlacesClientProxy
import com.stripe.android.ui.core.elements.autocomplete.model.FindAutocompletePredictionsResponse
import java.util.Locale

internal fun createInlineAutocompletePlacesClient(
    context: Context,
    googlePlacesApiKey: String?,
    errorReporter: ErrorReporter,
    isPlacesAvailable: Boolean,
): PlacesClientProxy? {
    if (!FeatureFlags.inlineAddressAutocompleteEnabled.isEnabled) return null
    if (!isPlacesAvailable) return null
    val apiKey = googlePlacesApiKey ?: return null
    return LazyPlacesClientProxy {
        PlacesClientProxy.create(
            context = context,
            googlePlacesApiKey = apiKey,
            errorReporter = errorReporter,
        )
    }
}

internal class LazyPlacesClientProxy(
    factory: () -> PlacesClientProxy,
) : PlacesClientProxy {
    private val delegate by lazy(factory)

    override suspend fun findAutocompletePredictions(
        query: String?,
        country: String,
        limit: Int,
    ): Result<FindAutocompletePredictionsResponse> = delegate.findAutocompletePredictions(query, country, limit)

    override suspend fun fetchPlace(placeId: String, locale: Locale): Result<Address> =
        delegate.fetchPlace(placeId, locale)

    override fun resetSession() {
        delegate.resetSession()
    }
}
