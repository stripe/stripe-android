package com.stripe.android.messagingelement

import androidx.compose.runtime.Composable

class PaymentMethodMessagingElement {

    fun configure(
        configuration: Configuration
    ) {

    }

    @Composable
    fun content() {

    }

    class Configuration(
        val amount: Long,
        val currency: String,
        val locale: String,
        val publishableKey: String,
        val countryCode: String?,
        val paymentMethodList: List<String>?,
        val referrer: String?
    ) {

        class Builder {
            private var amount: Long? = null
            private var currency: String? = null
            private var locale: String? = null
            private var publishableKey: String? = null
            private var countryCode: String? = null
            private var paymentMethodList: List<String>? = null
            private var referrer: String? = null

            fun amount(amount: Long) = apply {

            }

            fun currency() = apply {

            }
            fun locale() = apply {

            }
            fun publishableKey() = apply {

            }
            fun countryCode() = apply {

            }
            fun paymentMethodList() = apply {

            }
            fun referrer() = apply {

            }


        }
    }
}