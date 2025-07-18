package com.stripe.android.uicore.elements

import android.content.res.Resources
import androidx.annotation.RestrictTo
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material.Divider
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
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
    val dividerHeight = remember { mutableStateOf(0.dp) }
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
                            dividerHeight.value =
                                (it.height / Resources.getSystem().displayMetrics.density).dp
                        },
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
