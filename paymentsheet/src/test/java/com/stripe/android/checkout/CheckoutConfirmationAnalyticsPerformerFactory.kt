package com.stripe.android.checkout

import androidx.lifecycle.SavedStateHandle
import com.stripe.android.checkout.analytics.CheckoutEventReporter
import com.stripe.android.checkout.analytics.CheckoutEventReporterFactory
import com.stripe.android.link.account.LinkAccountHolder

internal object CheckoutConfirmationAnalyticsPerformerFactory {
    fun create(
        savedStateHandle: SavedStateHandle = SavedStateHandle(),
        stateHolder: CheckoutControllerStateHolder = CheckoutControllerStateFactory.createStateHolder(savedStateHandle),
        linkAccountHolder: LinkAccountHolder = LinkAccountHolder(savedStateHandle),
        checkoutEventReporter: CheckoutEventReporter = CheckoutEventReporterFactory.create(),
    ): CheckoutConfirmationAnalyticsPerformer = CheckoutConfirmationAnalyticsPerformer(
        savedStateHandle = savedStateHandle,
        stateHolder = stateHolder,
        linkAccountHolder = linkAccountHolder,
        checkoutEventReporter = checkoutEventReporter,
    )
}
