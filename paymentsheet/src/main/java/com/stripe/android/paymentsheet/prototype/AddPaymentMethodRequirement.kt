package com.stripe.android.paymentsheet.prototype

import com.stripe.android.model.PaymentIntent
import com.stripe.android.model.PaymentMethod

internal enum class AddPaymentMethodRequirement {
    /** A special case that indicates the payment method is always unsupported by PaymentSheet. */
    Unsupported {
        override fun meetsRequirements(metadata: ParsingMetadata): Boolean = false
    },

    /** A special case that indicates the payment method is unsupported by PaymentSheet when using SetupIntents or SFU. */
    UnsupportedForSetup {
        override fun meetsRequirements(metadata: ParsingMetadata): Boolean {
            return !metadata.hasIntentToSetup()
        }
    },

    /** Indicates that a payment method requires shipping information. */
    ShippingAddress {
        override fun meetsRequirements(metadata: ParsingMetadata): Boolean {
            val shipping = (metadata.stripeIntent as? PaymentIntent)?.shipping
            return shipping?.name != null &&
                shipping.address.line1 != null &&
                shipping.address.country != null &&
                shipping.address.postalCode != null
        }
    },

    /** Requires that the user declare support for asynchronous payment methods. */
    MerchantSupportsDelayedPaymentMethods {
        override fun meetsRequirements(metadata: ParsingMetadata): Boolean {
            return metadata.configuration.allowsDelayedPaymentMethods
        }
    },

    /** Requires that the FinancialConnections SDK has been linked. */
    FinancialConnectionsSdk {
        override fun meetsRequirements(metadata: ParsingMetadata): Boolean {
            return metadata.financialConnectionsAvailable()
        }
    },

    /** Requires a valid us bank verification method. */
    ValidUsBankVerificationMethod {
        override fun meetsRequirements(metadata: ParsingMetadata): Boolean {
            val pmo = metadata.stripeIntent.getPaymentMethodOptions()[PaymentMethod.Type.USBankAccount.code]
            val verificationMethod = (pmo as? Map<*, *>)?.get("verification_method") as? String
            val supportsVerificationMethod = verificationMethod in setOf("instant", "automatic")
            return supportsVerificationMethod || metadata.isDeferred
        }
    };

    abstract fun meetsRequirements(metadata: ParsingMetadata): Boolean
}
