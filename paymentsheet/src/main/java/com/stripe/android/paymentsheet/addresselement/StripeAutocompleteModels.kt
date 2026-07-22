package com.stripe.android.paymentsheet.addresselement

import com.stripe.android.core.model.StripeJsonUtils
import com.stripe.android.core.model.StripeModel
import com.stripe.android.core.model.parsers.ModelJsonParser
import kotlinx.parcelize.Parcelize
import org.json.JSONObject

@Parcelize
internal data class AutocompletePredictionsResult(
    val predictions: List<AutocompleteSuggestion>
) : StripeModel

@Parcelize
internal data class AutocompleteSuggestion(
    val placeId: String,
    val primaryText: String,
    val secondaryText: String,
    val address: StripeProxyAddress?
) : StripeModel

@Parcelize
internal data class StripeProxyAddress(
    val line1: String?,
    val line2: String?,
    val city: String?,
    val state: String?,
    val postalCode: String?,
    val country: String?
) : StripeModel

@Parcelize
internal data class PlaceDetailsResult(
    val address: StripeProxyAddress
) : StripeModel

internal object AutocompletePredictionsResponseJsonParser :
    ModelJsonParser<AutocompletePredictionsResult> {
    override fun parse(json: JSONObject): AutocompletePredictionsResult? {
        val suggestionsArray = json.optJSONArray("suggestions") ?: return null
        val predictions = (0 until suggestionsArray.length()).mapNotNull { i ->
            val suggestion = suggestionsArray.optJSONObject(i)
                ?: return@mapNotNull null
            val displayData = suggestion.optJSONObject("display_data")
            val primaryText =
                StripeJsonUtils.optString(suggestion, "primary_text")
                    ?: StripeJsonUtils.optString(displayData, "title")
                    ?: return@mapNotNull null
            val placeId = StripeJsonUtils.optString(suggestion, "place_id")
                ?: return@mapNotNull null
            AutocompleteSuggestion(
                placeId = placeId,
                primaryText = primaryText,
                secondaryText =
                    StripeJsonUtils.optString(suggestion, "secondary_text")
                        ?: StripeJsonUtils.optString(displayData, "subtitle")
                        ?: "",
                address = suggestion.optJSONObject("address")
                    ?.let { parseAddress(it) },
            )
        }
        return AutocompletePredictionsResult(predictions = predictions)
    }
}

internal object PlaceDetailsResponseJsonParser : ModelJsonParser<PlaceDetailsResult> {
    override fun parse(json: JSONObject): PlaceDetailsResult? {
        val addressJson = json.optJSONObject("address") ?: return null
        return PlaceDetailsResult(address = parseAddress(addressJson))
    }
}

private fun parseAddress(json: JSONObject): StripeProxyAddress {
    return StripeProxyAddress(
        line1 = StripeJsonUtils.optString(json, "line1"),
        line2 = StripeJsonUtils.optString(json, "line2"),
        city = StripeJsonUtils.optString(json, "city"),
        state = StripeJsonUtils.optString(json, "state"),
        postalCode = StripeJsonUtils.optString(json, "postal_code"),
        country = StripeJsonUtils.optString(json, "country"),
    )
}
