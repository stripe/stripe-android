package com.stripe.android.paymentsheet.elements

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
internal fun RowElementUI(
    enabled: Boolean,
    controller: RowController
) {
    val fields = controller.fields
    Row(
        Modifier
            .height(IntrinsicSize.Min)
            .fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        fields.forEachIndexed { index, field ->
            val lastItem = index != fields.size - 1
            SectionFieldElementUI(
                enabled,
                field,
                Modifier.fillMaxWidth(
                    (1f / fields.size.toFloat()).takeIf { lastItem } ?: 1f
                )
            )
            if (!lastItem) {
                val cardStyle = CardStyle(isSystemInDarkTheme())
                VeriticalDivider(
                    color = cardStyle.cardBorderColor,
                    thickness = cardStyle.cardBorderWidth,
                    modifier = Modifier
                        .padding(
                            horizontal = cardStyle.cardBorderWidth
                        )
                )
            }
        }
    }
}

@Composable
internal fun VeriticalDivider(
    color: Color,
    modifier: Modifier = Modifier,
    thickness: Dp = 1.dp,
) {
    Box(
        modifier = Modifier
            .fillMaxHeight()
            .width(thickness)
            .background(color)
    )
}
