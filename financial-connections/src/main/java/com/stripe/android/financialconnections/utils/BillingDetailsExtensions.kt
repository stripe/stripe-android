package com.stripe.android.financialconnections.utils

import com.stripe.android.financialconnections.FinancialConnectionsSheet.ElementsSessionContext.BillingDetails

/**
 * Creates API params for use with the Stripe core API.
 *
 * These params include the phone number and a nested address object.
 */
internal fun BillingDetails.toApiParams(): Map<String, Any> {
    val addressParams = address?.let { address ->
        buildMap {
            address.line1?.let { put("line1", it) }
            address.line2?.let { put("line2", it) }
            address.postalCode?.let { put("postal_code", it) }
            address.city?.let { put("city", it) }
            address.state?.let { put("state", it) }
            address.country?.let { put("country", it) }
        }.filterValues {
            it.isNotBlank()
        }
    }
    return mapOf(
        "name" to name,
        "phone" to phone,
        "address" to addressParams,
    ).filterNotNullValues()
}

/**
 * Creates API params for use with the consumer API.
 *
 * These params don't include the phone number and flatten the address.
 */
internal fun BillingDetails.toConsumerBillingAddressParams(): Map<String, Any> {
    val contactParams = buildMap {
        name?.let { put("name", it) }
    }.filter { entry ->
        entry.value.isNotBlank()
    }

    val addressParams = buildMap {
        address?.line1?.let { put("line_1", it) }
        address?.line2?.let { put("line_2", it) }
        address?.postalCode?.let { put("postal_code", it) }
        address?.city?.let { put("locality", it) }
        address?.state?.let { put("administrative_area", it) }
        address?.country?.let { put("country_code", it) }
    }.filterValues {
        it.isNotBlank()
    }

    return contactParams + addressParams
}
