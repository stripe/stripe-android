package com.stripe.android.utils

import android.content.Context
import com.stripe.android.ui.core.forms.resources.LpmRepository

internal fun initializedLpmRepository(context: Context): LpmRepository {
    val repository = LpmRepository(
        LpmRepository.LpmRepositoryArguments(context.resources)
    )

    val updateParams = LpmUpdateParamsFactory.create(
        paymentMethodTypes = repository.supportedPaymentMethods,
    )

    repository.update(updateParams)

    return repository
}
