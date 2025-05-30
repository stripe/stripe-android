package com.stripe.android.uicore.elements

import androidx.annotation.RestrictTo
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material.Divider
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.stripe.android.uicore.stripeColors
import com.stripe.android.uicore.stripeShapes

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@Composable
fun RowElementUI(
    enabled: Boolean,
    controller: RowController,
    hiddenIdentifiers: Set<IdentifierSpec>,
    lastTextFieldIdentifier: IdentifierSpec?,
    modifier: Modifier = Modifier,
) {
    val visibleFields = controller.fields.filter { !hiddenIdentifiers.contains(it.identifier) }
    val layoutDirection = LocalLayoutDirection.current
    // Only draw the row if there are items in the row that are not hidden, otherwise the entire
    // section will fail to draw
    if (visibleFields.isNotEmpty()) {
        Row(modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
            visibleFields.forEachIndexed { index, field ->
                val nextFocusDirection = if (index == visibleFields.lastIndex) {
                    FocusDirection.Down
                } else if (layoutDirection == LayoutDirection.Ltr) {
                    FocusDirection.Right
                } else {
                    FocusDirection.Left
                }

                val previousFocusDirection = if (index == 0) {
                    FocusDirection.Up
                } else if (layoutDirection == LayoutDirection.Ltr) {
                    FocusDirection.Left
                } else {
                    FocusDirection.Right
                }

                SectionFieldElementUI(
                    enabled,
                    field,
                    hiddenIdentifiers = hiddenIdentifiers,
                    lastTextFieldIdentifier = lastTextFieldIdentifier,
                    modifier = modifier.weight(1f),
                    nextFocusDirection = nextFocusDirection,
                    previousFocusDirection = previousFocusDirection
                )

                if (index != visibleFields.lastIndex) {
                    Divider(
                        modifier = Modifier
                            .fillMaxHeight()
                            .width(MaterialTheme.stripeShapes.borderStrokeWidth.dp),
                        color = MaterialTheme.stripeColors.componentDivider
                    )
                }
            }
        }
    }
}
