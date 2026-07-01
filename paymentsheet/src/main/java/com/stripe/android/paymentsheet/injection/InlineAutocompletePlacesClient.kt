package com.stripe.android.paymentsheet.injection

import android.content.Context
import com.stripe.android.core.utils.FeatureFlags
import com.stripe.android.payments.core.analytics.ErrorReporter
import com.stripe.android.ui.core.elements.autocomplete.PlacesClientProxy

/**
 * Creates a [PlacesClientProxy] for inline address autocomplete, or `null` when the feature is
 * disabled or no Google Places API key is configured.
 */
internal fun createInlineAutocompletePlacesClient(
    context: Context,
    googlePlacesApiKey: String?,
    errorReporter: ErrorReporter,
): PlacesClientProxy? {
    if (!FeatureFlags.inlineAddressAutocompleteEnabled.isEnabled) return null
    return googlePlacesApiKey?.let { apiKey ->
        PlacesClientProxy.create(
            context = context,
            googlePlacesApiKey = apiKey,
            errorReporter = errorReporter,
        )
    }
}
