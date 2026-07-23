package com.stripe.android.paymentsheet.addresselement

import android.text.SpannableString
import androidx.appcompat.app.AppCompatDelegate
import com.stripe.android.ui.core.elements.autocomplete.PlacesClientProxy
import com.stripe.android.ui.core.elements.autocomplete.model.AddressComponent
import com.stripe.android.ui.core.elements.autocomplete.model.AutocompletePrediction
import com.stripe.android.ui.core.elements.autocomplete.model.FetchPlaceResponse
import com.stripe.android.ui.core.elements.autocomplete.model.FindAutocompletePredictionsResponse
import com.stripe.android.ui.core.elements.autocomplete.model.Place
import java.util.Locale
import java.util.UUID

internal class StripeHostedPlacesClientProxy(
    private val stripeAutocompleteApiService: StripeAutocompleteApiService,
    private val googlePlacesApiKey: String?,
) : PlacesClientProxy {
    private var sessionToken: String = UUID.randomUUID().toString()
    private val cachedAddresses = mutableMapOf<String, StripeProxyAddress>()

    override suspend fun findAutocompletePredictions(
        query: String?,
        country: String,
        limit: Int
    ): Result<FindAutocompletePredictionsResponse> {
        if (query.isNullOrBlank()) {
            return Result.success(FindAutocompletePredictionsResponse(emptyList()))
        }
        val locale = AppCompatDelegate.getApplicationLocales()[0]?.toLanguageTag()
            ?: Locale.getDefault().toLanguageTag()
        return stripeAutocompleteApiService.findAutocompletePredictions(
            query = query,
            country = country,
            sessionToken = sessionToken,
            locale = locale,
            googleApiKey = googlePlacesApiKey,
        ).map { result ->
            cachedAddresses.clear()
            result.predictions.forEach { suggestion ->
                suggestion.address?.let { cachedAddresses[suggestion.placeId] = it }
            }
            FindAutocompletePredictionsResponse(
                autocompletePredictions = result.predictions.map { suggestion ->
                    AutocompletePrediction(
                        primaryText = SpannableString(suggestion.primaryText),
                        secondaryText = SpannableString(suggestion.secondaryText),
                        placeId = suggestion.placeId,
                    )
                }.take(limit)
            )
        }
    }

    override suspend fun fetchPlace(placeId: String): Result<FetchPlaceResponse> {
        val cachedAddress = cachedAddresses[placeId]
        if (cachedAddress != null) {
            resetSession()
            return Result.success(cachedAddress.toFetchPlaceResponse())
        }
        return stripeAutocompleteApiService.fetchPlaceDetails(
            placeId = placeId,
            sessionToken = sessionToken,
        ).onSuccess {
            resetSession()
        }.map { result ->
            result.address.toFetchPlaceResponse()
        }
    }

    override fun resetSession() {
        sessionToken = UUID.randomUUID().toString()
        cachedAddresses.clear()
    }
}

private fun StripeProxyAddress.toFetchPlaceResponse(): FetchPlaceResponse {
    // Omit COUNTRY — the form already has the user's selected country.
    val components = buildList {
        line1?.let { add(AddressComponent(it, it, listOf(Place.Type.STREET_NUMBER.value))) }
        line2?.let { add(AddressComponent(it, it, listOf(Place.Type.PREMISE.value))) }
        city?.let { add(AddressComponent(it, it, listOf(Place.Type.LOCALITY.value))) }
        state?.let {
            add(AddressComponent(it, it, listOf(Place.Type.ADMINISTRATIVE_AREA_LEVEL_1.value)))
        }
        postalCode?.let { add(AddressComponent(it, it, listOf(Place.Type.POSTAL_CODE.value))) }
    }
    return FetchPlaceResponse(Place(components))
}
