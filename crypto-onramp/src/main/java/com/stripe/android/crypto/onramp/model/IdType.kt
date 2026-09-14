package com.stripe.android.crypto.onramp.model

import com.stripe.android.crypto.onramp.ExperimentalCryptoOnramp

/**
 * Represents possible types of customer identification.
 */
@ExperimentalCryptoOnramp
enum class IdType(internal val value: String) {
    /** A United States Social Security Number. */
    SocialSecurityNumber("social_security_number"),

    /** A Canadian Social Insurance Number. */
    CanadianSocialInsuranceNumber("ca_sin"),

    /** A Colombian Número de Identificación Tributaria. */
    ColombianTaxIdentificationNumber("co_nit"),

    /** A Philippines Taxpayer Identification Number. */
    PhilippinesTaxpayerIdentificationNumber("ph_tin"),
}
