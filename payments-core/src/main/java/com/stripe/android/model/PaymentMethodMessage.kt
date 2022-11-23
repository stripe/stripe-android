package com.stripe.android.model

import androidx.annotation.RestrictTo
import com.stripe.android.core.model.StripeModel
import kotlinx.parcelize.Parcelize

@Parcelize
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
data class PaymentMethodMessage internal constructor(
    val displayHtml: String,
    val learnMoreUrl: String
) : StripeModel
