package com.stripe.android.ui.core.analytics

import androidx.annotation.RestrictTo
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import com.stripe.android.core.Logger
import com.stripe.android.uicore.BuildConfig
import com.stripe.android.uicore.analytics.LocalUiEventReporter

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
val LocalPaymentsUiEventReporter = compositionLocalOf<PaymentsUiEventReporter> {
    EmptyPaymentsUiEventReporter
}

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@Composable
fun ReportablePaymentsUi(
    eventReporter: PaymentsUiEventReporter,
    content: @Composable () -> Unit
) {
    CompositionLocalProvider(
        LocalPaymentsUiEventReporter provides eventReporter,
        LocalUiEventReporter provides eventReporter
    ) {
        content()
    }
}

private object EmptyPaymentsUiEventReporter : PaymentsUiEventReporter {
    private val logger: Logger
        get() = Logger.getInstance(BuildConfig.DEBUG)

    override fun onFieldInteracted() {
        logger.debug("PaymentsUiEventReporter.onFieldInteracted() event not reported")
    }

    override fun onCardNumberCompleted() {
        logger.debug("PaymentsUiEventReporter.onCardNumberCompleted() event not reported")
    }

    override fun onAutofillEvent(type: String) {
        logger.debug("PaymentsUiEventReporter.onAutofillEvent(name = $type) event not reported")
    }
}
