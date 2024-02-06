package com.stripe.android.ui.core.analytics

import androidx.annotation.RestrictTo
import com.stripe.android.uicore.analytics.UiEventReporter

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
interface PaymentsUiEventReporter : UiEventReporter {
    fun onCardNumberCompleted()
}
