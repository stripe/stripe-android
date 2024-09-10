package com.stripe.android.financialconnections.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class ShareNetworkedAccountsResponse(
    @SerialName("next_pane") val nextPane: FinancialConnectionsSessionManifest.Pane? = null,
    @SerialName("display_text") val display: Display? = null
)
