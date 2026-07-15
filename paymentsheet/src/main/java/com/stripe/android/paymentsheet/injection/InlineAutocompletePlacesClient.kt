@file:Suppress("MatchingDeclarationName")

package com.stripe.android.paymentsheet.injection

import android.content.Context
import com.stripe.android.core.utils.FeatureFlags
import com.stripe.android.payments.core.analytics.ErrorReporter
import com.stripe.android.paymentsheet.addresselement.StripeHostedPlacesClientProxy
import com.stripe.android.ui.core.elements.autocomplete.PlacesClientProxy
import com.stripe.android.ui.core.elements.autocomplete.model.FetchPlaceResponse
import com.stripe.android.ui.core.elements.autocomplete.model.FindAutocompletePredictionsResponse

internal interface HostedPlacesClientProxyProvider {
    fun createStripeHostedProxy(): PlacesClientProxy
}

internal fun createInlineAutocompletePlacesClient(
    context: Context,
    googlePlacesApiKey: String?,
    errorReporter: ErrorReporter,
    isPlacesAvailable: Boolean,
): PlacesClientProxy? {
    if (!FeatureFlags.inlineAddressAutocompleteEnabled.isEnabled) return null
    if (!isPlacesAvailable) return null
    val apiKey = googlePlacesApiKey ?: return null
    return LazyPlacesClientProxy(
        factory = {
            PlacesClientProxy.create(
                context = context,
                googlePlacesApiKey = apiKey,
                errorReporter = errorReporter,
            )
        },
        hostedFactory = {
            StripeHostedPlacesClientProxy(context = context, googleApiKey = apiKey)
        }
    )
}

private class LazyPlacesClientProxy(
    factory: () -> PlacesClientProxy,
    private val hostedFactory: () -> PlacesClientProxy,
) : PlacesClientProxy, HostedPlacesClientProxyProvider {
    private val delegate by lazy(factory)

    override suspend fun findAutocompletePredictions(
        query: String?,
        country: String,
        limit: Int,
    ): Result<FindAutocompletePredictionsResponse> = delegate.findAutocompletePredictions(query, country, limit)

    override suspend fun fetchPlace(placeId: String): Result<FetchPlaceResponse> = delegate.fetchPlace(placeId)

    override fun createStripeHostedProxy(): PlacesClientProxy = hostedFactory()
}
