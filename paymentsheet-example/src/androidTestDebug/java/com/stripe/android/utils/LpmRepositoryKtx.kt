package com.stripe.android.utils

import android.content.Context
import com.stripe.android.ui.core.PaymentSheetMode
import com.stripe.android.ui.core.forms.resources.LpmRepository

internal fun initializedLpmRepository(context: Context): LpmRepository {
    val repository = LpmRepository(
        LpmRepository.LpmRepositoryArguments(context.resources)
    )

    repository.update(
        mode = PaymentSheetMode.Payment(
            amount = 1000L,
            currency = "usd",
        ),
        setupFutureUsage = null,
        expectedLpms = repository.supportedPaymentMethods,
        serverLpmSpecs = null,
    )

    return repository
}
