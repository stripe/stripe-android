package com.stripe.android.paymentelement.confirmation.spay

import android.os.Parcelable
import com.stripe.android.paymentelement.confirmation.ConfirmationHandler
import com.stripe.android.payments.samsungpay.SamsungPayEnvironment
import com.stripe.android.payments.samsungpay.SamsungPayLauncher
import kotlinx.parcelize.Parcelize

@Parcelize
internal data class SamsungPayConfirmationOption(
    val config: SamsungPayLauncher.Config,
) : ConfirmationHandler.Option
