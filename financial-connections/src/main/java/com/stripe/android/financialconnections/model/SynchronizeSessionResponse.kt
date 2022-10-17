package com.stripe.android.financialconnections.model

import kotlinx.serialization.Serializable

@Serializable
internal data class SynchronizeSessionResponse(
    val manifest: FinancialConnectionsSessionManifest,
    val mobile: String?,
    val text: String?
)
