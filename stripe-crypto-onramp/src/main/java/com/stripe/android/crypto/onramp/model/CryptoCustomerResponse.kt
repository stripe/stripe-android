package com.stripe.android.crypto.onramp.model

import androidx.annotation.RestrictTo
import com.stripe.android.core.model.StripeModel
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable

@Parcelize
@Serializable
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
internal data class CryptoCustomerResponse(
    val id: String
) : StripeModel
