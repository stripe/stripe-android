package com.stripe.android.ui.core.elements

import androidx.annotation.RestrictTo
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Divider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource

@Composable
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
fun SectionElementUI(
    enabled: Boolean,
    element: SectionElement,
    hiddenIdentifiers: List<IdentifierSpec>,
    lastTextFieldIdentifier: IdentifierSpec?
) {
    if (!hiddenIdentifiers.contains(element.identifier)) {
        val controller = element.controller

        val error by controller.error.collectAsState(null)
        val sectionErrorString = error?.let {
            it.formatArgs?.let { args ->
                stringResource(
                    it.errorMessage,
                    *args
                )
            } ?: stringResource(it.errorMessage)
        }

        Section(controller.label, sectionErrorString) {
            element.fields.forEachIndexed { index, field ->
                SectionFieldElementUI(
                    enabled,
                    field,
                    hiddenIdentifiers = hiddenIdentifiers,
                    lastTextFieldIdentifier = lastTextFieldIdentifier
                )
                if (index != element.fields.size - 1) {
                    Divider(
                        color = CardStyle.cardBorderColor,
                        thickness = CardStyle.cardBorderWidth,
                        modifier = Modifier.padding(
                            horizontal = CardStyle.cardBorderWidth
                        )
                    )
                }
            }
        }
    }
}
