package com.stripe.android.paymentsheet.utils

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import com.stripe.android.paymentsheet.analytics.EventReporter
import com.stripe.android.ui.core.elements.events.LocalCardBrandDisallowedReporter
import com.stripe.android.ui.core.elements.events.LocalCardNumberCompletedEventReporter
import com.stripe.android.uicore.elements.LocalAutofillEventReporter

@Composable
internal fun EventReporterProvider(
    eventReporter: EventReporter,
    content: @Composable () -> Unit
) {
    CompositionLocalProvider(
        LocalAutofillEventReporter provides eventReporter::onAutofill,
        LocalCardNumberCompletedEventReporter provides eventReporter::onCardNumberCompleted,
        LocalCardBrandDisallowedReporter provides eventReporter::onDisallowedCardBrandEntered
    ) {
        content()
    }
}
