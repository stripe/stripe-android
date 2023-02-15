package com.stripe.android.utils

import com.stripe.android.ui.core.PaymentSheetMode
import com.stripe.android.ui.core.forms.resources.LpmRepository

internal object LpmUpdateParamsFactory {

    fun create(
        paymentMethodTypes: List<String> = listOf("card"),
        serverLpmSpecs: String? = null,
    ): LpmRepository.UpdateParams = LpmRepository.UpdateParams(
        mode = PaymentSheetMode.Payment,
        setupFutureUse = null,
        expectedLpms = paymentMethodTypes,
        serverLpmSpecs = serverLpmSpecs,
    )
}
