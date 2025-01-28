package com.stripe.android.paymentelement.embedded.form

import androidx.compose.runtime.Composable
import com.stripe.android.paymentsheet.analytics.EventReporter
import com.stripe.android.paymentsheet.utils.EventReporterProvider
import com.stripe.android.paymentsheet.verticalmode.DefaultVerticalModeFormInteractor
import com.stripe.android.paymentsheet.verticalmode.VerticalModeFormUI

@Composable
internal fun FormActivityUI(
    interactor: DefaultVerticalModeFormInteractor,
    eventReporter: EventReporter
) {
    EventReporterProvider(eventReporter) {
        VerticalModeFormUI(
            interactor = interactor,
            showsWalletHeader = false
        )
    }
}
