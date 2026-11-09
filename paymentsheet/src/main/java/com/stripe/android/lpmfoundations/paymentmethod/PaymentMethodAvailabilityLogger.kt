package com.stripe.android.lpmfoundations.paymentmethod

import com.stripe.android.core.utils.UserFacingLogger

internal fun logUnavailablePaymentMethods(
    metadata: PaymentMethodMetadata,
    logger: UserFacingLogger,
) {
    val supportedTypes = metadata.supportedPaymentMethodTypes()
    // Wallet availability is determined separately from the payment method registry.
    metadata.stripeIntent.paymentMethodTypes.distinct().filterNot { code ->
        code in supportedTypes || WalletType.entries.any { it.code == code }
    }.forEach { code ->
        val definition = PaymentMethodRegistry.definitionsByCode[code]
        val reasons = definition?.unmetRequirements(metadata)?.map { it.errorMessage }
            ?: listOf(AddPaymentMethodRequirement.Unsupported.errorMessage)
        if (reasons.isNotEmpty()) {
            logger.logWarningWithoutPii(
                "Payment Element could not offer $code:\n" + reasons.joinToString("\n") { "  * $it" }
            )
        }
    }
}
