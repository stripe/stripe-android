package com.stripe.android.paymentsheet.repositories

import com.stripe.android.model.StripeIntent
import com.stripe.android.paymentsheet.analytics.EventReporter
import com.stripe.android.paymentsheet.model.ClientSecret
import com.stripe.android.ui.core.forms.resources.LpmRepository
import com.stripe.android.ui.core.forms.resources.ResourceRepository

internal suspend fun initializeRepositoryAndGetStripeIntent(
    lpmResourceRepository: ResourceRepository<LpmRepository>,
    stripeIntentRepository: StripeIntentRepository,
    clientSecret: ClientSecret,
    eventReporter: EventReporter
): StripeIntent {
    val value = stripeIntentRepository.get(clientSecret)
    lpmResourceRepository.getRepository().update(
        value.intent.paymentMethodTypes,
        value.formUI
    )

    when (lpmResourceRepository.getRepository().serverSpecLoadingState) {
        is LpmRepository.ServerSpecState.ServerNotParsed -> {
            eventReporter.onLpmSpecFailure()
        }
        is LpmRepository.ServerSpecState.ServerParsed,
        is LpmRepository.ServerSpecState.Uninitialized,
        is LpmRepository.ServerSpecState.NoServerSpec -> {
        }
    }

    return value.intent
}
