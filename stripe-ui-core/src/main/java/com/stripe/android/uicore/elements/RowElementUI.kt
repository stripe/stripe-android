package com.stripe.android.uicore.elements

import androidx.annotation.RestrictTo
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material.Divider
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.dp
import com.stripe.android.uicore.stripeColors
import com.stripe.android.uicore.stripeShapes

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@Composable
fun RowElementUI(
    enabled: Boolean,
    controller: RowController,
    hiddenIdentifiers: Set<FormFieldId>,
    lastTextFieldIdentifier: FormFieldId?,
    modifier: Modifier = Modifier,
) {
    val visibleFields = controller.fields.filter { !hiddenIdentifiers.contains(it.identifier) }
    val dividerHeight = remember { mutableIntStateOf(0) }
    // Only draw the row if there are items in the row that are not hidden, otherwise the entire
    // section will fail to draw
    if (visibleFields.isNotEmpty()) {
        Row(modifier = Modifier.fillMaxWidth().wrapContentSize()) {
            visibleFields.forEachIndexed { index, field ->
                SectionFieldElementUI(
                    enabled,
                    field,
                    hiddenIdentifiers = hiddenIdentifiers,
                    lastTextFieldIdentifier = lastTextFieldIdentifier,
                    modifier = modifier
                        .weight(1.0f / visibleFields.size.toFloat())
                        .onSizeChanged {
                            dividerHeight.intValue = it.height
                        },
                )

                if (index != visibleFields.lastIndex) {
                    Divider(
                        modifier = Modifier
                            // Read the measured height during layout, not composition. Otherwise
                            // the first field measurement recomposes the entire row of text fields.
                            .layout { measurable, constraints ->
                                val height = dividerHeight.intValue.coerceIn(
                                    constraints.minHeight,
                                    constraints.maxHeight,
                                )
                                val placeable = measurable.measure(
                                    constraints.copy(minHeight = height, maxHeight = height)
                                )
                                layout(placeable.width, placeable.height) {
                                    placeable.placeRelative(0, 0)
                                }
                            }
                            .width(MaterialTheme.stripeShapes.borderStrokeWidth.dp),
                        color = MaterialTheme.stripeColors.componentDivider
                    )
                }
            }
        }
    }
}
