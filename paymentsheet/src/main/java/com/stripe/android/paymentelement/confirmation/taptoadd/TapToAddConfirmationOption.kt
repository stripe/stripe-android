package com.stripe.android.paymentelement.confirmation.taptoadd

import com.stripe.android.paymentelement.confirmation.ConfirmationHandler
import kotlinx.parcelize.Parcelize

@Parcelize
internal data class TapToAddConfirmationOption(
    val mode: Mode,
) : ConfirmationHandler.Option {
    enum class Mode {
        Complete,
        Continue
    }
}
