package com.stripe.android.lpmfoundations.paymentmethod

import com.stripe.android.model.PaymentIntent
import com.stripe.android.model.PaymentMethod

internal enum class AddPaymentMethodRequirement {
    /** A special case that indicates the payment method is always unsupported by PaymentSheet. */
    Unsupported {
        override fun isMetBy(metadata: PaymentMethodMetadata): Boolean = false
    },

    /** Indicates the payment method is unsupported by PaymentSheet when using SetupIntents or SFU. */
    UnsupportedForSetup {
        override fun isMetBy(metadata: PaymentMethodMetadata): Boolean {
            return !metadata.hasIntentToSetup()
        }
    },

    /** Indicates that a payment method requires shipping information. */
    ShippingAddress {
        override fun isMetBy(metadata: PaymentMethodMetadata): Boolean {
            if (metadata.allowsPaymentMethodsRequiringShippingAddress) {
                return true
            }

            val shipping = (metadata.stripeIntent as? PaymentIntent)?.shipping
            return shipping?.name != null &&
                shipping.address.line1 != null &&
                shipping.address.country != null &&
                shipping.address.postalCode != null
        }
    },

    /** Requires that the developer declare support for asynchronous payment methods. */
    MerchantSupportsDelayedPaymentMethods {
        override fun isMetBy(metadata: PaymentMethodMetadata): Boolean {
            return metadata.allowsDelayedPaymentMethods
        }
    },

    /** Requires that the FinancialConnections SDK has been linked. */
    FinancialConnectionsSdk {
        override fun isMetBy(metadata: PaymentMethodMetadata): Boolean {
            return metadata.financialConnectionsAvailable
        }
    },

    /** Requires a valid us bank verification method. */
    ValidUsBankVerificationMethod {
        override fun isMetBy(metadata: PaymentMethodMetadata): Boolean {
            val pmo = metadata.stripeIntent.getPaymentMethodOptions()[PaymentMethod.Type.USBankAccount.code]
            val verificationMethod = (pmo as? Map<*, *>)?.get("verification_method") as? String
            val supportsVerificationMethod = verificationMethod in setOf("instant", "automatic")
            val isDeferred = metadata.stripeIntent.clientSecret == null
            return supportsVerificationMethod || isDeferred
        }
    };

    abstract fun isMetBy(metadata: PaymentMethodMetadata): Boolean
}
