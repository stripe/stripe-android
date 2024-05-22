@file:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)

package com.stripe.android.uicore.elements

import androidx.annotation.RestrictTo
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Divider
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.stripe.android.uicore.stripeColors
import com.stripe.android.uicore.stripeShapes

@Composable
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
fun SectionElementUI(
    enabled: Boolean,
    element: SectionElement,
    hiddenIdentifiers: Set<IdentifierSpec>,
    lastTextFieldIdentifier: IdentifierSpec?,
    nextFocusDirection: FocusDirection = FocusDirection.Down,
    previousFocusDirection: FocusDirection = FocusDirection.Up
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

        val elementsInsideCard = element.fields.filter {
            !it.shouldRenderOutsideCard
        }
        val elementsOutsideCard = element.fields.filter {
            it.shouldRenderOutsideCard
        }

        Section(
            controller.label,
            sectionErrorString,
            contentOutsideCard = {
                elementsOutsideCard.forEach { field ->
                    SectionFieldElementUI(
                        enabled,
                        field,
                        hiddenIdentifiers = hiddenIdentifiers,
                        lastTextFieldIdentifier = lastTextFieldIdentifier,
                        nextFocusDirection = nextFocusDirection,
                        previousFocusDirection = previousFocusDirection
                    )
                }
            },
            contentInCard = {
                elementsInsideCard.forEachIndexed { index, field ->
                    SectionFieldElementUI(
                        enabled,
                        field,
                        hiddenIdentifiers = hiddenIdentifiers,
                        lastTextFieldIdentifier = lastTextFieldIdentifier,
                        nextFocusDirection = nextFocusDirection,
                        previousFocusDirection = previousFocusDirection
                    )
                    if (index != elementsInsideCard.lastIndex) {
                        Divider(
                            color = MaterialTheme.stripeColors.componentDivider,
                            thickness = MaterialTheme.stripeShapes.borderStrokeWidth.dp,
                            modifier = Modifier.padding(
                                horizontal = MaterialTheme.stripeShapes.borderStrokeWidth.dp
                            )
                        )
                    }
                }
            }
        )
    }
}
