package com.stripe.android.crypto.onramp.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class DeleteWalletRequestParams(
    @SerialName("wallet_token")
    val walletToken: String,
    val credentials: CryptoCustomerRequestParams.Credentials
)
