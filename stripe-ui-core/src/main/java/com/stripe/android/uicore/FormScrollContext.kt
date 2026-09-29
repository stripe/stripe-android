package com.stripe.android.uicore

import androidx.annotation.RestrictTo
import androidx.compose.foundation.ScrollState
import androidx.compose.runtime.MutableIntState
import androidx.compose.runtime.staticCompositionLocalOf

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class FormScrollContext(
    val scrollState: ScrollState,
    private val viewportTopYState: MutableIntState,
) {
    val viewportTopY: Int get() = viewportTopYState.intValue
}

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
val LocalFormScrollContext = staticCompositionLocalOf<FormScrollContext?> { null }
