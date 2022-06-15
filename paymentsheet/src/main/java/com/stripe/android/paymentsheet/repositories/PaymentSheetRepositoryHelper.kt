package com.stripe.android.paymentsheet.repositories

import com.stripe.android.model.StripeIntent
import com.stripe.android.paymentsheet.model.ClientSecret
import com.stripe.android.ui.core.forms.resources.ResourceRepository

internal suspend fun initializeRepositoryAndGetStripeIntent(
    resourceRepository: ResourceRepository,
    stripeIntentRepository: StripeIntentRepository,
    clientSecret: ClientSecret
): StripeIntent {
    val value = stripeIntentRepository.get(clientSecret)
    resourceRepository.getLpmRepository().update(
        value.intent.paymentMethodTypes,
        value.formUI
    )
    return value.intent
}
