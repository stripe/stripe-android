package com.stripe.android.uicore.elements

import androidx.annotation.RestrictTo
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@Composable
fun SectionFieldElementUI(
    enabled: Boolean,
    field: SectionFieldElement,
    modifier: Modifier = Modifier,
    hiddenIdentifiers: Set<IdentifierSpec> = emptySet(),
    lastTextFieldIdentifier: IdentifierSpec?,
) {
    if (!hiddenIdentifiers.contains(field.identifier)) {
        (field.sectionFieldErrorController() as? SectionFieldComposable)?.ComposeUI(
            enabled,
            field,
            modifier,
            hiddenIdentifiers,
            lastTextFieldIdentifier
        )
    }
}
