package com.stripe.android.paymentelement.confirmation

import android.os.Bundle
import javax.inject.Inject

internal interface IsEligibleForConfirmationChallenge {
    operator fun invoke(
        confirmationOption: PaymentMethodConfirmationOption,
        metadata: Bundle
    ): Boolean
}

internal class DefaultIsEligibleForConfirmationChallenge @Inject constructor(
    private val isCardPaymentMethodForChallenge: IsCardPaymentMethodForChallenge
) : IsEligibleForConfirmationChallenge {
    override fun invoke(
        confirmationOption: PaymentMethodConfirmationOption,
        metadata: Bundle
    ): Boolean {
        return when (confirmationOption) {
            is PaymentMethodConfirmationOption.New -> {
                isCardPaymentMethodForChallenge(confirmationOption)
            }
            is PaymentMethodConfirmationOption.Saved -> {
                isCardPaymentMethodForChallenge(confirmationOption) &&
                    metadata.getBoolean("newPMTransformedForConfirmation", false)
            }
        }
    }
}
