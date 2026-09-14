package com.stripe.android.googlepaylauncher

import androidx.annotation.RestrictTo
import com.google.android.gms.wallet.callback.IntermediatePaymentData
import com.stripe.android.core.model.CountryCode
import com.stripe.android.core.model.StripeJsonUtils.optString
import dev.drewhamilton.poko.Poko
import org.json.JSONObject

/**
 * Represents an update in the Google Pay sheet that requires a response on how to change the sheet
 *
 * @param callbackTrigger represents what change to the sheet caused a callback trigger
 * @param shippingAddress represents the shipping address selected in teh sheet
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@Poko
class GooglePayPaymentDataUpdate(
    val callbackTrigger: CallbackTrigger?,
    val shippingAddress: ShippingAddress?,
) {
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    enum class CallbackTrigger {
        /**
         * Callback is called when the Google Pay sheet is initialized
         */
        Initialize,

        /**
         * Callback is called when the shipping address changes in the sheet
         */
        ShippingAddress,

        /**
         * Callback is called when the shipping option changes in the sheet
         */
        ShippingOption,

        /**
         * Callback is called when the offer changes in the sheet
         */
        Offer,
    }

    /**
     * Represents an update in the Google Pay sheet that requires a response on how to change the sheet
     *
     * @param administrativeArea country subdivision, such as a state or province
     * @param countryCode ISO 3166-1 alpha-2 country code
     * @param locality City, town, neighborhood, or suburb
     * @param postalCode The redacted postal code based on the country. For Canada and the UK, this contains
     *   only the first three characters. For US, this contains the first five digits.
     * @param iso3166AdministrativeArea ISO 3166-2 administrative area code corresponding to administrativeArea.
     *   Only present if the shipping address format is FULL-ISO3166.
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    @Poko
    class ShippingAddress(
        val administrativeArea: String,
        val countryCode: String,
        val locality: String,
        val postalCode: String,
        val iso3166AdministrativeArea: String?,
    ) {
        /*
         * Google Pay returns `administrativeArea` as a freeform string (whatever the buyer saved in their wallet),
         * which can be a full state name like "CALIFORNIA" instead of the ISO code "CA". When we request the
         * FULL-ISO3166 address format, Google additionally returns `iso3166AdministrativeArea` with the ISO 3166-2
         * subdivision code (e.g. "US-CA"). For US addresses we parse the subdivision out of that code so the
         * state is consistently a 2-char ISO code. We scope this to the US to avoid changing behavior for other
         * countries. See RUN_OCSBUYERXP-1121.
         */
        val normalizedAdministrativeArea: String
            get() {
                val separated = iso3166AdministrativeArea?.split(SEPARATOR)?.takeIf { it.size >= 2 }
                    ?: return administrativeArea

                val (country, subdivision) = separated

                return if (country == CountryCode.US.value) {
                    subdivision.uppercase()
                } else {
                    administrativeArea
                }
            }

        private companion object {
            const val SEPARATOR = "-"
        }
    }

    internal companion object {
        fun fromIntermediatePaymentData(
            intermediatePaymentData: IntermediatePaymentData,
        ): GooglePayPaymentDataUpdate {
            return fromJson(JSONObject(intermediatePaymentData.toJson()))
        }

        private fun fromJson(json: JSONObject): GooglePayPaymentDataUpdate {
            return GooglePayPaymentDataUpdate(
                callbackTrigger = json.optString(FIELD_CALLBACK_TRIGGER)
                    .takeIf { it.isNotEmpty() }
                    ?.let { value ->
                        CallbackTrigger.entries.firstOrNull {
                            it.name.replace(UPPERCASE_WORD_BOUNDARY, "_$1")
                                .trimStart('_')
                                .uppercase() == value
                        }
                    },
                shippingAddress = json.optJSONObject(FIELD_SHIPPING_ADDRESS)?.let { addressJson ->
                    ShippingAddress(
                        administrativeArea = optString(addressJson, FIELD_ADMINISTRATIVE_AREA)
                            .orEmpty(),
                        countryCode = optString(addressJson, FIELD_COUNTRY_CODE).orEmpty(),
                        locality = optString(addressJson, FIELD_LOCALITY).orEmpty(),
                        postalCode = optString(addressJson, FIELD_POSTAL_CODE).orEmpty(),
                        iso3166AdministrativeArea =
                            optString(addressJson, FIELD_IOS_3166_ADMINISTRATIVE_AREA)
                                ?.takeIf { it.isNotEmpty() },
                    )
                },
            )
        }

        private const val FIELD_CALLBACK_TRIGGER = "callbackTrigger"
        private const val FIELD_SHIPPING_ADDRESS = "shippingAddress"
        private const val FIELD_COUNTRY_CODE = "countryCode"
        private const val FIELD_ADMINISTRATIVE_AREA = "administrativeArea"
        private const val FIELD_LOCALITY = "locality"
        private const val FIELD_POSTAL_CODE = "postalCode"
        private const val FIELD_IOS_3166_ADMINISTRATIVE_AREA = "iso3166AdministrativeArea"
        private val UPPERCASE_WORD_BOUNDARY = Regex("([A-Z])")
    }
}
