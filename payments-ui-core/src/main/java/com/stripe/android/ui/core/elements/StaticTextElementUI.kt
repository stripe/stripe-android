package com.stripe.android.ui.core.elements

import androidx.annotation.RestrictTo
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import com.stripe.android.uicore.elements.H6Text
import com.stripe.android.uicore.strings.resolve

@Composable
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
fun StaticTextElementUI(
    element: StaticTextElement,
    modifier: Modifier,
) {
    H6Text(
        text = element.text.resolve(),
        modifier = modifier
            .semantics(mergeDescendants = true) {} // makes it a separate accessible item
    )
}
