package com.stripe.android.crypto.onramp.example.network

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class CreateOnrampSessionRequest(
    @SerialName("ui_mode")
    val uiMode: String,
    @SerialName("payment_token")
    val paymentToken: String,
    @SerialName("source_amount")
    val sourceAmount: Double,
    @SerialName("source_currency")
    val sourceCurrency: String,
    @SerialName("destination_currency")
    val destinationCurrency: String,
    @SerialName("destination_network")
    val destinationNetwork: String,
    @SerialName("wallet_address")
    val walletAddress: String,
    @SerialName("crypto_customer_id")
    val cryptoCustomerId: String,
    @SerialName("customer_ip_address")
    val customerIpAddress: String
)
