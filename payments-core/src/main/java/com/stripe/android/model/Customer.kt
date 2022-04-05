package com.stripe.android.model

import com.stripe.android.core.model.StripeModel
import kotlinx.parcelize.Parcelize

/**
 * Model for a Stripe Customer object
 */
@Parcelize
data class Customer internal constructor(
    val id: String?,
    val defaultSource: String?,
    val shippingInformation: ShippingInformation?,
    val sources: List<CustomerPaymentSource>,
    val hasMore: Boolean,
    val totalCount: Int?,
    val url: String?,
    val description: String?,
    val email: String?,
    val liveMode: Boolean
) : StripeModel {
    fun getSourceById(sourceId: String): CustomerPaymentSource? {
        return sources.firstOrNull { it.id == sourceId }
    }
}
