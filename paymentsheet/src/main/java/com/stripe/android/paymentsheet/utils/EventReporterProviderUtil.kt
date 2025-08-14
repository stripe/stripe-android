package com.stripe.android.paymentsheet.utils

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import com.stripe.android.paymentsheet.analytics.EventReporter
import com.stripe.android.ui.core.cardscan.CardScanEventReporterWrapper
import com.stripe.android.ui.core.cardscan.LocalCardScanEventsReporter
import com.stripe.android.ui.core.elements.events.LocalAnalyticsEventReporter
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
        LocalCardBrandDisallowedReporter provides eventReporter::onDisallowedCardBrandEntered,
        LocalAnalyticsEventReporter provides eventReporter::onAnalyticsEvent,
        LocalCardScanEventsReporter provides CardScanEventReporterWrapper(
            onCardScanStarted = eventReporter::onCardScanStarted,
            onCardScanSucceeded = eventReporter::onCardScanSucceeded,
            onCardScanFailed = eventReporter::onCardScanFailed,
            onCardScanCancelled = eventReporter::onCardScanCancelled,
            onCardScanApiCheck = eventReporter::onCardScanApiCheck
        ),
    ) {
        content()
    }
}
