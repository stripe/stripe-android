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
    val transactionDetails: CheckoutTransactionDetails,
    @SerialName("ui_mode")
    val uiMode: String
)

@Serializable
data class CheckoutTransactionDetails(
    @SerialName("destination_currency")
    val destinationCurrency: String,
    @SerialName("destination_exchange_amount")
    val destinationExchangeAmount: String,
    @SerialName("destination_network")
    val destinationNetwork: String,
    val fees: CheckoutFees,
    @SerialName("last_error")
    val lastError: String? = null,
    @SerialName("lock_wallet_address")
    val lockWalletAddress: Boolean,
    @SerialName("quote_expiration")
    val quoteExpiration: String,
    @SerialName("source_currency")
    val sourceCurrency: String,
    @SerialName("source_exchange_amount")
    val sourceExchangeAmount: String,
    @SerialName("supported_destination_currencies")
    val supportedDestinationCurrencies: List<String>,
    @SerialName("supported_destination_networks")
    val supportedDestinationNetworks: List<String>,
    @SerialName("transaction_id")
    val transactionId: String? = null,
    @SerialName("transaction_limit")
    val transactionLimit: Long,
    @SerialName("wallet_address")
    val walletAddress: String,
    @SerialName("wallet_addresses")
    val walletAddresses: List<String>? = null
)

@Serializable
data class CheckoutFees(
    @SerialName("network_fee_amount")
    val networkFeeAmount: String,
    @SerialName("transaction_fee_amount")
    val transactionFeeAmount: String
)
