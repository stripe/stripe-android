package com.stripe.android.paymentelement.confirmation

internal class FakeIsEligibleForConfirmationChallenge(
    private var isEligible: Boolean = true
) : IsEligibleForConfirmationChallenge {
    override fun invoke(confirmationOption: PaymentMethodConfirmationOption): Boolean {
        return isEligible
    }

    fun setEligible(eligible: Boolean) {
        isEligible = eligible
    }
}
