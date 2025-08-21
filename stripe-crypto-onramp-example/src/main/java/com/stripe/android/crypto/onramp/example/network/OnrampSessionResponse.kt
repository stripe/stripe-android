package com.stripe.android.crypto.onramp.example.network

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

@Serializable
data class OnrampSessionResponse(
    val id: String,
    @SerialName("client_secret")
    val clientSecret: String,
    val created: Long,
    @SerialName("crypto_customer_id")
    val cryptoCustomerId: String,
    @SerialName("finish_url")
    val finishUrl: String? = null,
    @SerialName("is_apple_pay")
    val isApplePay: Boolean,
    @SerialName("kyc_details_provided")
    val kycDetailsProvided: Boolean,
    val metadata: JsonObject,
    @SerialName("payment_method")
    val paymentMethod: String,
    @SerialName("preferred_payment_method")
    val preferredPaymentMethod: String? = null,
    @SerialName("preferred_region")
    val preferredRegion: String? = null,
    @SerialName("redirect_url")
    val redirectUrl: String,
    @SerialName("skip_quote_screen")
    val skipQuoteScreen: Boolean,
    @SerialName("source_total_amount")
    val sourceTotalAmount: String,
    val status: String,
    @SerialName("transaction_details")
    val transactionDetails: OnrampTransactionDetails,
    @SerialName("ui_mode")
    val uiMode: String
)

@Serializable
data class OnrampTransactionDetails(
    @SerialName("destination_currency")
    val destinationCurrency: String? = null,
    @SerialName("destination_exchange_amount")
    val destinationExchangeAmount: String? = null,
    @SerialName("destination_network")
    val destinationNetwork: String? = null,
    val fees: OnrampFees,
    @SerialName("last_error")
    val lastError: String? = null,
    @SerialName("lock_wallet_address")
    val lockWalletAddress: Boolean,
    @SerialName("source_currency")
    val sourceCurrency: String? = null,
    @SerialName("source_exchange_amount")
    val sourceExchangeAmount: String? = null,
    @SerialName("supported_destination_currencies")
    val supportedDestinationCurrencies: List<String>? = null,
    @SerialName("supported_destination_networks")
    val supportedDestinationNetworks: List<String>? = null,
    @SerialName("transaction_id")
    val transactionId: String? = null,
    @SerialName("transaction_limit")
    val transactionLimit: Long,
    @SerialName("wallet_address")
    val walletAddress: String? = null,
    @SerialName("wallet_addresses")
    val walletAddresses: List<String>? = null
)

@Serializable
data class OnrampFees(
    @SerialName("network_fee_amount")
    val networkFeeAmount: String,
    @SerialName("transaction_fee_amount")
    val transactionFeeAmount: String
)
