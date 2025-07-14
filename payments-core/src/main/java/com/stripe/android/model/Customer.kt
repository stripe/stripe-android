package com.stripe.android.model

import android.os.Parcelable
import dev.drewhamilton.poko.Poko
import kotlinx.parcelize.Parcelize

/**
 * Model for a Stripe Customer object
 */
@Parcelize
@Poko
class Customer internal constructor(
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
) : Parcelable {
    fun getSourceById(sourceId: String): CustomerPaymentSource? {
        return sources.firstOrNull { it.id == sourceId }
    }
}
