package com.stripe.android.paymentelement.confirmation.taptoadd

import com.stripe.android.common.taptoadd.TapToAddMode
import com.stripe.android.paymentelement.confirmation.ConfirmationHandler
import kotlinx.parcelize.Parcelize

@Parcelize
internal data class TapToAddConfirmationOption(
    val mode: TapToAddMode,
) : ConfirmationHandler.Option
