package com.stripe.android.ui.core.elements

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.Divider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.constraintlayout.compose.Dimension

@Composable
internal fun RowElementUI(
    enabled: Boolean,
    controller: RowController,
    hiddenIdentifiers: List<IdentifierSpec>
) {
    val fields = controller.fields

    // An attempt was made to do this with a row, and a vertical divider created with a box.
    // The row had a height of IntrinsicSize.Min, and the box/vertical divider filled the height
    // when adding in the trailing icon this broke and caused the overall height of the row to
    // increase.  The approach only supports two items in the row.
    ConstraintLayout {
        // Create references for the composables to constrain
        val (field1Ref, dividerRef, field2Ref) = createRefs()

        val field1 = fields[0]
        SectionFieldElementUI(
            enabled,
            field1,
            Modifier
                .constrainAs(field1Ref) {
                    start.linkTo(parent.start)
                    top.linkTo(parent.top)
                }
                .fillMaxWidth(0.5f),
            hiddenIdentifiers
        )
        val cardStyle = CardStyle(isSystemInDarkTheme())

        Divider(
            modifier = Modifier
                .constrainAs(dividerRef) {
                    start.linkTo(field1Ref.end)
                    top.linkTo(parent.top)
                    bottom.linkTo(parent.bottom)
                    height = (Dimension.fillToConstraints)
                }
                .padding(
                    horizontal = cardStyle.cardBorderWidth
                )
                .width(cardStyle.cardBorderWidth)
                .background(cardStyle.cardBorderColor)
        )
        val field2 = fields[1]
        SectionFieldElementUI(
            enabled,
            field2,
            Modifier
                .constrainAs(field2Ref) {
                    start.linkTo(dividerRef.end)
                    top.linkTo(parent.top)
                }
                .fillMaxWidth(0.5f),
            hiddenIdentifiers
        )
    }
}
