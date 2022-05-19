package com.stripe.android.ui.core.elements

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.Divider
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.constraintlayout.compose.Dimension
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

    // Only draw the row if the items in the row are not hidden, otherwise the entire
    // section will fail to draw
    if (fields.map { it.identifier }.any { !hiddenIdentifiers.contains(it) }) {
        // An attempt was made to do this with a row, and a vertical divider created with a box.
        // The row had a height of IntrinsicSize.Min, and the box/vertical divider filled the height
        // when adding in the trailing icon this broke and caused the overall height of the row to
        // increase.  By using the constraint layout the vertical divider does not negatively effect
        // the size of the row.
        ConstraintLayout {
            // Create references for the composables to constrain
            val fieldRefs = fields.map { createRef() }
            val dividerRefs = fields.map { createRef() }

            fields.forEachIndexed { index, field ->
                SectionFieldElementUI(
                    enabled,
                    field,
                    hiddenIdentifiers = hiddenIdentifiers,
                    lastTextFieldIdentifier = lastTextFieldIdentifier,
                    modifier = Modifier
                        .constrainAs(fieldRefs[index]) {
                            if (index == 0) {
                                start.linkTo(parent.start)
                            } else {
                                start.linkTo(dividerRefs[index - 1].end)
                            }
                            top.linkTo(parent.top)
                        }
                        .fillMaxWidth(
                            (1f / numVisibleFields.toFloat())
                        )
                )

                if (!hiddenIdentifiers.contains(field.identifier) && index != fields.lastIndex) {
                    Divider(
                        modifier = Modifier
                            .constrainAs(dividerRefs[index]) {
                                start.linkTo(fieldRefs[index].end)
                                top.linkTo(parent.top)
                                bottom.linkTo(parent.bottom)
                                height = (Dimension.fillToConstraints)
                            }
                            .padding(
                                horizontal = MaterialTheme.paymentsShapes.borderStrokeWidth.dp
                            )
                            .width(MaterialTheme.paymentsShapes.borderStrokeWidth.dp),
                        color = MaterialTheme.paymentsColors.componentDivider
                    )
                }
            }
        }
    }
}
