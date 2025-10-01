package com.stripe.android.crypto.onramp.model

import androidx.annotation.RestrictTo
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
internal data class CryptoWalletRequestParams(
    @SerialName("wallet_address")
    val walletAddress: String,
    val network: CryptoNetwork,
    val credentials: CryptoCustomerRequestParams.Credentials
)
