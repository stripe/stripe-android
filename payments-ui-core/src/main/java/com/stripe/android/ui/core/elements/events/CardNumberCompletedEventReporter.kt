package com.stripe.android.ui.core.elements.events

import androidx.annotation.RestrictTo
import androidx.compose.runtime.staticCompositionLocalOf
import com.stripe.android.uicore.BuildConfig

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
fun interface CardNumberCompletedEventReporter {
    fun onCardNumberCompleted(): Unit
}

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
val LocalCardNumberCompletedEventReporter = staticCompositionLocalOf<CardNumberCompletedEventReporter> {
    EmptyCardEventReporter
}

private object EmptyCardEventReporter : CardNumberCompletedEventReporter {
    override fun onCardNumberCompleted() {
        if (BuildConfig.DEBUG) {
            error("CardNumberCompletedEventReporter.onCardNumberCompleted() was not reported")
        }
    }
}
