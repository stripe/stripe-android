package com.stripe.android.paymentsheet.elements

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Divider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource

@Composable
internal fun SectionElementUI(
    enabled: Boolean,
    element: SectionElement,
    hiddenIdentifiers: List<IdentifierSpec>?,
) {
    if (hiddenIdentifiers?.contains(element.identifier) == false) {
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
                SectionFieldElementUI(enabled, field, hiddenIdentifiers = hiddenIdentifiers)
                if (index != element.fields.size - 1) {
                    val cardStyle = CardStyle(isSystemInDarkTheme())
                    Divider(
                        color = cardStyle.cardBorderColor,
                        thickness = cardStyle.cardBorderWidth,
                        modifier = Modifier.padding(
                            horizontal = cardStyle.cardBorderWidth
                        )
                    )
                }
            }
        }
    }
}
