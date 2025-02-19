package com.stripe.android.uicore.elements

import android.content.res.Resources
import androidx.annotation.RestrictTo
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material.Divider
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.layout.onSizeChanged
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
    lastTextFieldIdentifier: IdentifierSpec?
) {
    val visibleFields = controller.fields.filter { !hiddenIdentifiers.contains(it.identifier) }
    val dividerHeight = remember { mutableStateOf(0.dp) }
    val layoutDirection = LocalLayoutDirection.current
    // Only draw the row if there are items in the row that are not hidden, otherwise the entire
    // section will fail to draw
    if (visibleFields.isNotEmpty()) {
        Row(modifier = Modifier.fillMaxWidth()) {
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
                    modifier = Modifier
                        .weight(1.0f / visibleFields.size.toFloat())
                        .onSizeChanged {
                            dividerHeight.value =
                                (it.height / Resources.getSystem().displayMetrics.density).dp
                        },
                    nextFocusDirection = nextFocusDirection,
                    previousFocusDirection = previousFocusDirection
                )

                if (index != visibleFields.lastIndex) {
                    Divider(
                        modifier = Modifier
                            .height(dividerHeight.value)
                            .width(MaterialTheme.stripeShapes.borderStrokeWidth.dp),
                        color = MaterialTheme.stripeColors.componentDivider
                    )
                }
            }
        }
    }
}
