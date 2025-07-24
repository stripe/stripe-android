package com.stripe.android.challenge

import android.os.Parcelable
import com.stripe.android.paymentelement.confirmation.PaymentMethodConfirmationOption
import kotlinx.parcelize.Parcelize

@Parcelize
internal data class PassiveChallengeArgs(
    val newPaymentMethodConfirmationOption: PaymentMethodConfirmationOption.New
): Parcelable
