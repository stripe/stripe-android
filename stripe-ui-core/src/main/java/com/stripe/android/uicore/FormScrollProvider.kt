package com.stripe.android.uicore

import androidx.annotation.RestrictTo
import androidx.compose.foundation.ScrollState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@Composable
fun FormScrollProvider(
    scrollState: ScrollState,
    content: @Composable (Modifier) -> Unit,
) {
    val viewportTopYState = remember { mutableIntStateOf(0) }
    val scrollContext = remember(scrollState) { FormScrollContext(scrollState, viewportTopYState) }
    val viewportModifier = Modifier.onGloballyPositioned {
        viewportTopYState.intValue = it.positionInRoot().y.toInt()
    }
    CompositionLocalProvider(LocalFormScrollContext provides scrollContext) {
        content(viewportModifier)
    }
}
