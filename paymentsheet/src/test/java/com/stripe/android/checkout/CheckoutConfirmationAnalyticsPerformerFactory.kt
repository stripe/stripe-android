package com.stripe.android.checkout

import androidx.lifecycle.SavedStateHandle
import com.stripe.android.checkout.analytics.CheckoutEventReporter
import com.stripe.android.checkout.analytics.CheckoutEventReporterFactory
import com.stripe.android.link.account.LinkAccountHolder
import com.stripe.android.paymentsheet.analytics.EventReporter
import com.stripe.android.paymentsheet.analytics.FakeEventReporter

internal object CheckoutConfirmationAnalyticsPerformerFactory {
    fun create(
        eventReporter: EventReporter = FakeEventReporter(),
        savedStateHandle: SavedStateHandle = SavedStateHandle(),
        stateHolder: CheckoutControllerStateHolder = CheckoutControllerStateFactory.createStateHolder(savedStateHandle),
        linkAccountHolder: LinkAccountHolder = LinkAccountHolder(savedStateHandle),
        checkoutEventReporter: CheckoutEventReporter = CheckoutEventReporterFactory.create(),
    ): CheckoutConfirmationAnalyticsPerformer = CheckoutConfirmationAnalyticsPerformer(
        eventReporter = eventReporter,
        savedStateHandle = savedStateHandle,
        stateHolder = stateHolder,
        linkAccountHolder = linkAccountHolder,
        checkoutEventReporter = checkoutEventReporter,
    )
}
