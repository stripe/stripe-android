package com.stripe.android.model

import com.stripe.android.core.model.StripeModel
import kotlinx.parcelize.Parcelize

/**
 * Model representing a shipping address object
 */
@Parcelize
data class ShippingInformation constructor(
    val address: Address? = null,
    val name: String? = null,
    val phone: String? = null
) : StripeModel, StripeParamsModel {

    override fun toParamMap(): Map<String, Any> {
        return listOf(
            PARAM_NAME to name,
            PARAM_PHONE to phone,
            PARAM_ADDRESS to address?.toParamMap()
        )
            .mapNotNull { (first, second) -> second?.let { Pair(first, it) } }
            .toMap()
    }

    companion object {
        private const val PARAM_ADDRESS = "address"
        private const val PARAM_NAME = "name"
        private const val PARAM_PHONE = "phone"
    }
}
