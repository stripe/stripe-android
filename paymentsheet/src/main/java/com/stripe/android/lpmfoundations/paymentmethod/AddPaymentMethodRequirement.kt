package com.stripe.android.lpmfoundations.paymentmethod

import com.stripe.android.model.LinkMode
import com.stripe.android.model.PaymentMethod.Type.USBankAccount
import com.stripe.android.model.SetupIntent
import com.stripe.android.paymentsheet.PaymentSheet.BillingDetailsCollectionConfiguration.CollectionMode

internal enum class AddPaymentMethodRequirement {
    /** A special case that indicates the payment method is always unsupported by PaymentSheet. */
    Unsupported {
        override val errorMessage: String =
            "This payment method is not currently supported by the Payment Element."

        override fun isMetBy(metadata: PaymentMethodMetadata, code: String): Boolean = false
    },

    /** Indicates the payment method is unsupported by PaymentSheet when using SetupIntents or SFU. */
    UnsupportedForSetup {
        override val errorMessage: String =
            "This payment method is not supported with a SetupIntent or setup_future_usage."

        override fun isMetBy(metadata: PaymentMethodMetadata, code: String): Boolean {
            return !metadata.hasIntentToSetup(code)
        }
    },

    /** Indicates the payment method is unsupported by PaymentSheet when using SetupIntents. */
    UnsupportedForSetupIntent {
        override val errorMessage: String =
            "This payment method is not supported with a SetupIntent."

        override fun isMetBy(metadata: PaymentMethodMetadata, code: String): Boolean {
            return metadata.stripeIntent !is SetupIntent
        }
    },

    /** Requires that the developer declare support for asynchronous payment methods. */
    MerchantSupportsDelayedPaymentMethods {
        override val errorMessage: String =
            "Configuration.allowsDelayedPaymentMethods must be set to true."

        override fun isMetBy(metadata: PaymentMethodMetadata, code: String): Boolean {
            return metadata.allowsDelayedPaymentMethods
        }
    },

    /** Requires that the FinancialConnections SDK has been linked. */
    FinancialConnectionsSdk {
        override val errorMessage: String =
            "The Financial Connections SDK must be included. " +
                "See https://docs.stripe.com/payments/accept-a-payment?platform=android&payment-ui=mobile#android-ach"

        override fun isMetBy(metadata: PaymentMethodMetadata, code: String): Boolean {
            return metadata.financialConnectionsAvailability != null
        }
    },

    /** Requires a valid us bank verification method. */
    ValidUsBankVerificationMethod {
        override val errorMessage: String =
            "US bank accounts require an automatic, instant, or instant_or_skip verification method."

        override fun isMetBy(metadata: PaymentMethodMetadata, code: String): Boolean {
            // Verification method is always 'automatic' for deferred intents
            val isDeferred = metadata.stripeIntent.clientSecret == null
            return isDeferred || supportedVerificationMethodForNonDeferredIntent(metadata)
        }

        private fun supportedVerificationMethodForNonDeferredIntent(
            metadata: PaymentMethodMetadata,
        ): Boolean {
            val pmo = metadata.stripeIntent.getPaymentMethodOptions()[USBankAccount.code]
            val verificationMethod = (pmo as? Map<*, *>)?.get("verification_method") as? String
            val supportsVerificationMethod = verificationMethod in setOf("automatic", "instant", "instant_or_skip")
            return supportsVerificationMethod
        }
    },

    /** Requires that Instant Debits are possible for this transaction. */
    InstantDebits {
        override val errorMessage: String =
            "Instant debits require Link, bank onboarding, and email collection or an attached default email."

        override fun isMetBy(metadata: PaymentMethodMetadata, code: String): Boolean {
            return metadata.linkConfiguration.shouldDisplay &&
                metadata.linkMode != LinkMode.LinkCardBrand &&
                metadata.supportsMobileInstantDebitsFlow
        }
    },

    /** Requires that LinkCardBrand is possible for this transaction. */
    LinkCardBrand {
        override val errorMessage: String =
            "Link card brand requires Link card brand mode, bank onboarding, and email collection or an " +
                "attached default email."

        override fun isMetBy(metadata: PaymentMethodMetadata, code: String): Boolean {
            return metadata.linkConfiguration.shouldDisplay &&
                metadata.linkMode == LinkMode.LinkCardBrand &&
                metadata.supportsMobileInstantDebitsFlow
        }
    },

    /** Requires the buyer's Stripe balance financial accounts, which load only with a CustomerSession. */
    StripeBalanceFinancialAccounts {
        override val errorMessage: String =
            "Stripe balance requires a customer configured with a CustomerSession, and the customer's financial " +
                "accounts to load."

        override fun isMetBy(metadata: PaymentMethodMetadata, code: String): Boolean {
            return metadata.stripeBalanceFinancialAccounts != null
        }
    };

    abstract fun isMetBy(metadata: PaymentMethodMetadata, code: String): Boolean

    abstract val errorMessage: String
}

private val PaymentMethodMetadata.supportsMobileInstantDebitsFlow: Boolean
    get() {
        return isInstantDebitsOnboardingEnabled && canShowBankForm
    }

private val PaymentMethodMetadata.canShowBankForm: Boolean
    get() {
        val collectsEmail = billingDetailsCollectionConfiguration.email != CollectionMode.Never
        val attachDefaults = billingDetailsCollectionConfiguration.attachDefaultsToPaymentMethod
        val hasDefaultValue = attachDefaults && !defaultBillingDetails?.email.isNullOrBlank()
        return collectsEmail || hasDefaultValue
    }
