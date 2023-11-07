package com.stripe.android.financialconnections.model

import com.stripe.android.financialconnections.model.FinancialConnectionsSessionManifest.Pane
import kotlinx.serialization.Required
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class NetworkedAccountsList(
    @SerialName(value = "data") @Required val data: List<PartnerAccount>,

    @SerialName(value = "display") val display: Display? = null,

    @SerialName(value = "next_pane_on_add_account") val nextPaneOnAddAccount: Pane? = null,

    @SerialName(value = "partner_to_core_auths") val partnerToCoreAuths: Map<String, String>? = null
)
