package com.stripe.android.model

import androidx.annotation.RestrictTo
import com.stripe.android.core.model.StripeModel
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@Parcelize
@Serializable
data class UpdateAvailableIncentives(
    @SerialName("data") val data: List<LinkConsumerIncentive>,
) : StripeModel
