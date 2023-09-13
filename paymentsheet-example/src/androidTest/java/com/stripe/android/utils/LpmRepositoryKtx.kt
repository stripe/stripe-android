package com.stripe.android.utils

import android.content.Context
import com.stripe.android.model.PaymentMethod
import com.stripe.android.testing.PaymentIntentFactory
import com.stripe.android.ui.core.forms.resources.LpmRepository

internal fun initializedLpmRepository(context: Context): LpmRepository {
    val repository = LpmRepository(
        LpmRepository.LpmRepositoryArguments(context.resources)
    )

    repository.update(
        stripeIntent = PaymentIntentFactory.create(
            paymentMethodTypes = PaymentMethod.Type.values().map { it.code },
        ),
        serverLpmSpecs = null,
    )

    return repository
}
