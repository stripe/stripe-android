package com.stripe.android.challenge

import android.os.Parcelable
import com.stripe.android.paymentelement.confirmation.PaymentMethodConfirmationOption
import kotlinx.parcelize.Parcelize

internal sealed interface PassiveChallengeActivityResult : Parcelable {
    @Parcelize
    data class Success(
        val newPaymentMethodConfirmationOption: PaymentMethodConfirmationOption.New
    ): PassiveChallengeActivityResult

    @Parcelize
    data class Failed(
        val error: Throwable
    ): PassiveChallengeActivityResult
}
