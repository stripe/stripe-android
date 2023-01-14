package com.stripe.android.utils

import android.content.Context
import com.stripe.android.ui.core.forms.resources.LpmRepository

internal fun initializedLpmRepository(context: Context): LpmRepository {
    val repository = LpmRepository(
        LpmRepository.LpmRepositoryArguments(context.resources)
    )

    repository.update(
        stripeIntent = PaymentIntentFactory.create(
            paymentMethodTypes = repository.supportedPaymentMethods,
        ),
        serverLpmSpecs = null,
    )

    return repository
}
