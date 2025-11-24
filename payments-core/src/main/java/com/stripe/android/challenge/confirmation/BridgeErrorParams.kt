package com.stripe.android.challenge.confirmation

import com.stripe.android.core.model.StripeModel
import kotlinx.parcelize.Parcelize

@Parcelize
data class BridgeErrorParams(
    val message: String?,
    val type: String?,
    val code: String?
) : StripeModel
