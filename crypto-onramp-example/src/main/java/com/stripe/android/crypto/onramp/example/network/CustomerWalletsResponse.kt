package com.stripe.android.crypto.onramp.example.network

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class CustomerWalletsResponse(
    val count: Int,
    val data: List<CustomerWallet>
)

@Parcelize
@Serializable
data class CustomerWallet(
    val id: String,
    val network: String,
    @SerialName("wallet_address")
    val walletAddress: String,
    @SerialName("verified_ownership")
    val verifiedOwnership: Boolean
) : Parcelable
