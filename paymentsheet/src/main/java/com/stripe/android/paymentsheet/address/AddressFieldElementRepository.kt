package com.stripe.android.paymentsheet.address

import android.content.res.Resources
import androidx.annotation.VisibleForTesting
import com.stripe.android.paymentsheet.SectionFieldElement
import com.stripe.android.paymentsheet.forms.transform
import javax.inject.Inject

internal class AddressFieldElementRepository @Inject internal constructor(
    val resources: Resources
) {
    private val countryFieldMap = mutableMapOf<String, List<SectionFieldElement>?>()

    internal fun get(countryCode: String?) = countryCode?.let {
        countryFieldMap[it]
    } ?: countryFieldMap[DEFAULT_COUNTRY_CODE]

    internal fun init() {
        init(
            supportedCountries.associateWith { countryCode ->
                "addressinfo/$countryCode.json"
            }.mapValues { (_, assetFileName) ->
                requireNotNull(
                    parseAddressesSchema(
                        resources.assets.open(assetFileName)
                    ),
                )
            }
        )
    }

    @VisibleForTesting
    internal fun init(
        countryAddressSchemaPair: Map<String, List<AddressSchema>>
    ) {
        countryAddressSchemaPair.map { (countryCode, schemaList) ->
            countryCode to requireNotNull(
                schemaList
                    .transformToSpecFieldList()
                    .transform()
            )
        }.forEach { (countryCode, listElements) ->
            countryFieldMap[countryCode] = listElements
        }
    }

    companion object {
        @VisibleForTesting
        internal const val DEFAULT_COUNTRY_CODE = "ZZ"

        // Matches the Stripe-js-v3 code: https://git.corp.stripe.com/stripe-internal/stripe-js-v3/blob/e4ae99302dde74ccc6fcabdd1a58193f74e21ebf/src/lib/shared/checkoutSupportedCountries.js
        @VisibleForTesting
        internal val supportedCountries = setOf(
            // @formatter:off
            "AE", "AT", "AU", "BE", "BG", "BR", "CA", "CH", "CI", "CR", "CY", "CZ", "DE", "DK",
            "DO", "EE", "ES", "FI", "FR", "GB", "GI", "GR", "GT", "HK", "HU", "ID", "IE", "IN",
            "IT", "JP", "LI", "LT", "LU", "LV", "MT", "MX", "MY", "NL", "NO", "NZ", "PE", "PH",
            "PL", "PT", "RO", "SE", "SG", "SI", "SK", "SN", "TH", "TT", "US", "UY", "ZZ"
            // @formatter:on
        )
    }
}
