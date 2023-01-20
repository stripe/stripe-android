package com.stripe.android.uicore.elements

import androidx.annotation.RestrictTo
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@Composable
fun SectionFieldElementUI(
    enabled: Boolean,
    field: SectionFieldElement,
    modifier: Modifier = Modifier,
    hiddenIdentifiers: Set<IdentifierSpec> = emptySet(),
    lastTextFieldIdentifier: IdentifierSpec?,
    nextFocusDirection: FocusDirection = FocusDirection.Down,
    previousFocusDirection: FocusDirection = FocusDirection.Up
) {
    if (!hiddenIdentifiers.contains(field.identifier)) {
        (field.sectionFieldErrorController() as? SectionFieldComposable)?.ComposeUI(
            enabled,
            field,
            modifier,
            hiddenIdentifiers,
            lastTextFieldIdentifier,
            nextFocusDirection,
            previousFocusDirection
        )
    }
}
