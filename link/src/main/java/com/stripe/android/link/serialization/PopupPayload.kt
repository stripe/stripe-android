package com.stripe.android.link.serialization

import android.content.Context
import android.os.Build
import android.util.Base64
import com.stripe.android.link.LinkConfiguration
import com.stripe.android.model.PaymentIntent
import com.stripe.android.model.SetupIntent
import com.stripe.android.model.StripeIntent
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
internal data class PopupPayload(
    @SerialName("publishableKey")
    val publishableKey: String,

    @SerialName("stripeAccount")
    val stripeAccount: String?,

    @SerialName("merchantInfo")
    val merchantInfo: MerchantInfo,

    @SerialName("customerInfo")
    val customerInfo: CustomerInfo,

    @SerialName("paymentInfo")
    val paymentInfo: PaymentInfo?,

    @SerialName("appId")
    val appId: String,

    @SerialName("locale")
    val locale: String,

    @SerialName("paymentUserAgent")
    val paymentUserAgent: String,
) {
    @SerialName("path")
    val path: String = "mobile_pay"

    @SerialName("integrationType")
    val integrationType: String = "mobile"

    @SerialName("paymentObject")
    val paymentObject: String = "link_payment_method"

    @SerialName("loggerMetadata")
    val loggerMetadata: Map<String, String> = emptyMap()

    @SerialName("flags")
    val flags: Map<String, String> = emptyMap()

    @SerialName("experiments")
    val experiments: Map<String, String> = emptyMap()

    @Serializable
    data class MerchantInfo(
        @SerialName("businessName")
        val businessName: String,

        @SerialName("country")
        val country: String?,
    )

    @Serializable
    data class CustomerInfo(
        @SerialName("email")
        val email: String?,

        @SerialName("country")
        val country: String?,
    )

    @Serializable
    data class PaymentInfo(
        @SerialName("currency")
        val currency: String,

        @SerialName("amount")
        val amount: Long,
    )

    fun toUrl(): String {
        val json = PopupPayloadJson.encodeToString(serializer(), this)
        return baseUrl + Base64.encodeToString(json.encodeToByteArray(), Base64.NO_WRAP)
    }

    companion object {
        private const val baseUrl: String = "https://checkout.link.com/#"

        val PopupPayloadJson = Json { encodeDefaults = true }

        fun create(
            configuration: LinkConfiguration,
            context: Context,
            publishableKey: String,
            stripeAccount: String?,
            paymentUserAgent: String,
        ): PopupPayload {
            return configuration.toPopupPayload(
                context = context,
                publishableKey = publishableKey,
                stripeAccount = stripeAccount,
                paymentUserAgent = paymentUserAgent,
            )
        }

        private fun LinkConfiguration.toPopupPayload(
            context: Context,
            publishableKey: String,
            stripeAccount: String?,
            paymentUserAgent: String,
        ): PopupPayload {
            return PopupPayload(
                publishableKey = publishableKey,
                stripeAccount = stripeAccount,
                merchantInfo = MerchantInfo(
                    businessName = merchantName,
                    country = merchantCountryCode,
                ),
                customerInfo = CustomerInfo(
                    email = customerEmail,
                    country = customerBillingCountryCode ?: merchantCountryCode,
                ),
                paymentInfo = stripeIntent.toPaymentInfo(),
                appId = context.applicationInfo.packageName,
                locale = context.currentLocale(),
                paymentUserAgent = paymentUserAgent,
            )
        }

        private fun StripeIntent.toPaymentInfo(): PaymentInfo? {
            return when (this) {
                is PaymentIntent -> {
                    val currency = currency
                    val amount = amount
                    if (currency != null && amount != null) {
                        PaymentInfo(currency = currency, amount = amount)
                    } else {
                        null
                    }
                }

                is SetupIntent -> null
            }
        }

        private fun Context.currentLocale(): String {
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                resources.configuration.locales.get(0)
            } else {
                @Suppress("DEPRECATION")
                resources.configuration.locale
            }.country
        }
    }
}
