package com.stripe.android.model

import androidx.annotation.RestrictTo
import com.stripe.android.core.model.StripeModel
import kotlinx.parcelize.Parcelize

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@Parcelize
data class ConsumerShippingAddressesResponse(
    val addresses: List<ConsumerShippingAddress>,
) : StripeModel

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@Parcelize
data class ConsumerShippingAddress(
    val id: String,
    val isDefault: Boolean,
    val address: ConsumerPaymentDetails.BillingAddress,
) : StripeModel
