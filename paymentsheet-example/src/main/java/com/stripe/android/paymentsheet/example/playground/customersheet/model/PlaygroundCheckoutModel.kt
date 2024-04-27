package com.stripe.android.paymentsheet.example.playground.customersheet.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
class CustomerEphemeralKeyRequest private constructor(
    @SerialName("customer_type")
    val customerType: String?,
    @SerialName("customer_key_type")
    val customerKeyType: CustomerKeyType?,
    @SerialName("merchant_country_code")
    val merchantCountryCode: String?,
) {
    @Serializable
    enum class CustomerKeyType {
        @SerialName("customer_session")
        CustomerSession,

        @SerialName("legacy")
        Legacy;
    }

    class Builder {
        private var customerType: String? = null
        private var merchantCountryCode: String? = null

        fun customerType(customerType: String) = apply {
            this.customerType = customerType
        }

        fun merchantCountryCode(merchantCountryCode: String?) = apply {
            this.merchantCountryCode = merchantCountryCode
        }

        fun build(): CustomerEphemeralKeyRequest {
            return CustomerEphemeralKeyRequest(
                customerType = customerType,
                customerKeyType = CustomerKeyType.Legacy,
                merchantCountryCode = merchantCountryCode,
            )
        }
    }
}

@Serializable
data class CustomerEphemeralKeyResponse(
    @SerialName("publishableKey")
    val publishableKey: String,
    @SerialName("customerId")
    val customerId: String? = null,
    @SerialName("customerEphemeralKeySecret")
    val customerEphemeralKeySecret: String? = null,
    @SerialName("customerSessionClientSecret")
    val customerSessionClientSecret: String? = null,
)

@Serializable
data class CreateSetupIntentRequest(
    @SerialName("customer_id")
    val customerId: String,
    @SerialName("merchant_country_code")
    val merchantCountryCode: String,
)

@Serializable
data class CreateSetupIntentResponse(
    @SerialName("client_secret")
    val clientSecret: String,
)
