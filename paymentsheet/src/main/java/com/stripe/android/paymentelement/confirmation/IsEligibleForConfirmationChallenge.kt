package com.stripe.android.paymentelement.confirmation

import javax.inject.Inject

internal interface IsEligibleForConfirmationChallenge {
    operator fun invoke(
        confirmationOption: PaymentMethodConfirmationOption
    ): Boolean
}

internal class DefaultIsEligibleForConfirmationChallenge @Inject constructor(
    private val isCardPaymentMethodForChallenge: IsCardPaymentMethodForChallenge
) : IsEligibleForConfirmationChallenge {
    override fun invoke(
        confirmationOption: PaymentMethodConfirmationOption
    ): Boolean {
        return when (confirmationOption) {
            is PaymentMethodConfirmationOption.New -> {
                isCardPaymentMethodForChallenge(confirmationOption)
            }
            is PaymentMethodConfirmationOption.Saved -> {
                isCardPaymentMethodForChallenge(confirmationOption) &&
                    confirmationOption.newPMTransformedForConfirmation
            }
        }
    }
}
