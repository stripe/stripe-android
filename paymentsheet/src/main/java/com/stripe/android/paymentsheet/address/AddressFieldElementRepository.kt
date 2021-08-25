package com.stripe.android.paymentsheet.address

import android.content.res.Resources
import androidx.annotation.VisibleForTesting
import com.stripe.android.paymentsheet.SectionFieldElement
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class AddressFieldElementRepository @Inject internal constructor(
    val resources: Resources?
) {
    // This is needed for @Preview and inject does not support a constructor with default parameters.
    internal constructor() : this(null)

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
                        resources?.assets?.open(assetFileName)
                    ),
                )
            }
        )
    }

    @VisibleForTesting
    internal fun init(
        countryAddressSchemaPair: Map<String, List<CountryAddressSchema>>
    ) {
        countryAddressSchemaPair.map { (countryCode, schemaList) ->
            countryCode to requireNotNull(
                schemaList
                    .transformToElementList()
            )
        }.forEach { add(it.first, it.second) }
    }

    @VisibleForTesting
    internal fun add(countryCode: String, listElements: List<SectionFieldElement>) {
        countryFieldMap[countryCode] = listElements
    }

    companion object {
        @VisibleForTesting
        internal const val DEFAULT_COUNTRY_CODE = "ZZ"

        // Matches the Stripe-js-v3 code: https://git.corp.stripe.com/stripe-internal/stripe-js-v3/blob/master/src/lib/shared/checkoutSupportedCountries.js
        @VisibleForTesting
        internal val supportedCountries = setOf(
            // @formatter:off
            "AC", "AD", "AE", "AF", "AG", "AI", "AL", "AM", "AO", "AQ", "AR", "AT", "AU", "AW",
            "AX", "AZ", "BA", "BB", "BD", "BE", "BF", "BG", "BH", "BI", "BJ", "BL", "BM", "BN",
            "BO", "BQ", "BR", "BS", "BT", "BV", "BW", "BY", "BZ", "CA", "CD", "CF", "CG", "CH",
            "CI", "CK", "CL", "CM", "CN", "CO", "CR", "CV", "CW", "CY", "CZ", "DE", "DJ", "DK",
            "DM", "DO", "DZ", "EC", "EE", "EG", "EH", "ER", "ES", "ET", "FI", "FJ", "FK", "FO",
            "FR", "GA", "GB", "GD", "GE", "GF", "GG", "GH", "GI", "GL", "GM", "GN", "GP", "GQ",
            "GR", "GS", "GT", "GU", "GW", "GY", "HK", "HN", "HR", "HT", "HU", "ID", "IE", "IL",
            "IM", "IN", "IO", "IQ", "IS", "IT", "JE", "JM", "JO", "JP", "KE", "KG", "KH", "KI",
            "KM", "KN", "KR", "KW", "KY", "KZ", "LA", "LB", "LC", "LI", "LK", "LR", "LS", "LT",
            "LU", "LV", "LY", "MA", "MC", "MD", "ME", "MF", "MG", "MK", "ML", "MM", "MN", "MO",
            "MQ", "MR", "MS", "MT", "MU", "MV", "MW", "MX", "MY", "MZ", "NA", "NC", "NE", "NG",
            "NI", "NL", "NO", "NP", "NR", "NU", "NZ", "OM", "PA", "PE", "PF", "PG", "PH", "PK",
            "PL", "PM", "PN", "PR", "PS", "PT", "PY", "QA", "RE", "RO", "RS", "RU", "RW", "SA",
            "SB", "SC", "SE", "SG", "SH", "SI", "SJ", "SK", "SL", "SM", "SN", "SO", "SR", "SS",
            "ST", "SV", "SX", "SZ", "TA", "TC", "TD", "TF", "TG", "TH", "TJ", "TK", "TL", "TM",
            "TN", "TO", "TR", "TT", "TV", "TW", "TZ", "UA", "UG", "US", "UY", "UZ", "VA", "VC",
            "VE", "VG", "VN", "VU", "WF", "WS", "XK", "YE", "YT", "ZA", "ZM", "ZW", "ZZ",
            // @formatter:on
        )
    }
}
