package com.stripe.android.googlepaylauncher.callback

import com.google.android.gms.wallet.callback.IntermediatePaymentData
import com.stripe.android.core.model.StripeJsonUtils.optString
import dev.drewhamilton.poko.Poko
import org.json.JSONObject

/**
 * Payment data passed to [GooglePayPaymentDataChangedCallback] when the customer updates their
 * shipping address or shipping option.
 */
@Poko
class GooglePayIntermediatePaymentData internal constructor(
    val callbackTrigger: CallbackTrigger?,
    val shippingAddress: IntermediateShippingAddress?,
    val shippingOption: ShippingOptionSelection?,
) {
    enum class CallbackTrigger {
        INITIALIZE,
        SHIPPING_ADDRESS,
        SHIPPING_OPTION,
        OFFER,
    }

    internal companion object {
        fun fromIntermediatePaymentData(
            intermediatePaymentData: IntermediatePaymentData,
        ): GooglePayIntermediatePaymentData {
            return fromJson(JSONObject(intermediatePaymentData.toJson()))
        }

        fun fromJson(json: JSONObject): GooglePayIntermediatePaymentData {
            return GooglePayIntermediatePaymentData(
                callbackTrigger = json.optString("callbackTrigger").takeIf { it.isNotEmpty() }?.let { value ->
                    CallbackTrigger.entries.firstOrNull { it.name == value }
                },
                shippingAddress = json.optJSONObject("shippingAddress")?.let { addressJson ->
                    IntermediateShippingAddress(
                        administrativeArea = optString(addressJson, "administrativeArea").orEmpty(),
                        countryCode = optString(addressJson, "countryCode").orEmpty(),
                        locality = optString(addressJson, "locality").orEmpty(),
                        postalCode = optString(addressJson, "postalCode").orEmpty(),
                        iso3166AdministrativeArea = optString(addressJson, "iso3166AdministrativeArea")
                            ?.takeIf { it.isNotEmpty() },
                    )
                },
                shippingOption = json.optJSONObject("shippingOptionData")?.let { optionJson ->
                    ShippingOptionSelection(
                        id = optString(optionJson, "id").orEmpty(),
                    )
                },
            )
        }
    }
}

@Poko
class IntermediateShippingAddress internal constructor(
    val administrativeArea: String,
    val countryCode: String,
    val locality: String,
    val postalCode: String,
    val iso3166AdministrativeArea: String?,
)

@Poko
class ShippingOptionSelection internal constructor(
    val id: String,
)
