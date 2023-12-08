package com.stripe.android.uicore.elements

import androidx.annotation.RestrictTo
import androidx.compose.runtime.staticCompositionLocalOf
import com.stripe.android.core.Logger
import com.stripe.android.uicore.BuildConfig

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
sealed interface UiEvent {
    data class Autofill(val type: String) : UiEvent
    object CardBrandChoiceDropdownDisplayed : UiEvent
    data class CardBrandChoiceDropdownOpened(val selection: String?) : UiEvent
    data class CardBrandChoiceDropdownClosed(val selection: String?) : UiEvent
}

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
val LocalUiEventReporter = staticCompositionLocalOf(::defaultUiEventReporter)

internal fun defaultUiEventReporter(): (UiEvent) -> Unit {
    return { uiEvent ->
        Logger.getInstance(BuildConfig.DEBUG).debug("$uiEvent event not reported")
    }
}
