package com.stripe.android.uicore.address

import android.content.res.Resources
import androidx.annotation.RestrictTo
import androidx.annotation.VisibleForTesting
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Immutable repository to save the static schema of each country's address.
 */
@Singleton
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
open class AddressSchemaRepository @Inject constructor(
    val resources: Resources?
) {
    protected val countryAddressSchemaMap: Map<String, List<CountryAddressSchema>> by lazy {
        SUPPORTED_COUNTRIES.associateWith { countryCode ->
            "addressinfo/$countryCode.json"
        }.mapValues { (_, assetFileName) ->
            requireNotNull(
                parseAddressesSchema(
                    resources?.assets?.open(assetFileName)
                )
            )
        }
    }

    /**
     * Get the schemas related to a country's address.
     */
    fun getSchema(countryCode: String?): List<CountryAddressSchema>? = countryCode?.let {
        countryAddressSchemaMap[it]
    } ?: countryAddressSchemaMap[DEFAULT_COUNTRY_CODE]

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    companion object {
        const val DEFAULT_COUNTRY_CODE = "ZZ"

        // Matches the Stripe-js-v3 code: https://git.corp.stripe.com/stripe-internal/stripe-js-v3/blob/master/src/lib/shared/checkoutSupportedCountries.js
        @VisibleForTesting
        val SUPPORTED_COUNTRIES = setOf(
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
            "VE", "VG", "VN", "VU", "WF", "WS", "XK", "YE", "YT", "ZA", "ZM", "ZW", "ZZ"
            // @formatter:on
        )
    }
}
