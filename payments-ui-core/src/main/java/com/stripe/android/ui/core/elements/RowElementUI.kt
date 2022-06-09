package com.stripe.android.ui.core.elements

import android.content.res.Resources
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
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.dp
import com.stripe.android.ui.core.paymentsColors
import com.stripe.android.ui.core.paymentsShapes

@Composable
internal fun RowElementUI(
    enabled: Boolean,
    controller: RowController,
    hiddenIdentifiers: List<IdentifierSpec>,
    lastTextFieldIdentifier: IdentifierSpec?
) {
    val fields = controller.fields
    val numVisibleFields = fields.filter { !hiddenIdentifiers.contains(it.identifier) }.size
    val dividerHeight = remember { mutableStateOf(0.dp) }
    // Only draw the row if the items in the row are not hidden, otherwise the entire
    // section will fail to draw
    if (fields.map { it.identifier }.any { !hiddenIdentifiers.contains(it) }) {
        Row(modifier = Modifier.fillMaxWidth()) {
            fields.forEachIndexed { index, field ->
                SectionFieldElementUI(
                    enabled,
                    field,
                    hiddenIdentifiers = hiddenIdentifiers,
                    lastTextFieldIdentifier = lastTextFieldIdentifier,
                    modifier = Modifier
                        .weight(1.0f / numVisibleFields.toFloat())
                        .onSizeChanged {
                            dividerHeight.value =
                                (it.height / Resources.getSystem().displayMetrics.density).dp
                        }
                )

                if (index != fields.lastIndex) {
                    Divider(
                        modifier = Modifier
                            .height(dividerHeight.value)
                            .width(MaterialTheme.paymentsShapes.borderStrokeWidth.dp),
                        color = MaterialTheme.paymentsColors.componentDivider
                    )
                }
            }
        }
    }
}
