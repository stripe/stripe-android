package com.stripe.android.uicore.elements

import androidx.annotation.RestrictTo
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection

/**
 * Indicates a class could be drawn as a Composable within SectionFieldElementUI.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
fun interface SectionFieldComposable {
    @Composable
    fun ComposeUI(
        enabled: Boolean,
        field: SectionFieldElement,
        modifier: Modifier,
        hiddenIdentifiers: Set<IdentifierSpec>,
        lastTextFieldIdentifier: IdentifierSpec?,
        nextFocusDirection: FocusDirection,
        previousFocusDirection: FocusDirection
    )
}
