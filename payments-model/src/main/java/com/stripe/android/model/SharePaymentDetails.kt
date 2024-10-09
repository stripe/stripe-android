package com.stripe.android.model

import androidx.annotation.RestrictTo
import com.stripe.android.core.model.StripeModel
import kotlinx.parcelize.Parcelize

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@Parcelize
data class SharePaymentDetails(
    val paymentMethodId: String,
    val encodedPaymentMethod: String,
) : StripeModel
