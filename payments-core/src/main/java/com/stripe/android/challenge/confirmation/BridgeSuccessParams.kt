package com.stripe.android.challenge.confirmation

import com.stripe.android.core.model.StripeModel
import kotlinx.parcelize.Parcelize

@Parcelize
internal data class BridgeSuccessParams(val clientSecret: String) : StripeModel
