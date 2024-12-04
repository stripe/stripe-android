package com.stripe.android.financialconnections.model

import com.stripe.android.model.LinkConsumerIncentive
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class UpdateAvailableIncentives(
    @SerialName("data") val data: List<LinkConsumerIncentive>,
)
