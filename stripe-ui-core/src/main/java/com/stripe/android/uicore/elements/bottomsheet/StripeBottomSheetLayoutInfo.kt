package com.stripe.android.uicore.elements.bottomsheet

import androidx.annotation.RestrictTo
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.MaterialTheme
import androidx.compose.material.ModalBottomSheetDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.stripe.android.uicore.stripeShapes

@RestrictTo(RestrictTo.Scope.LIBRARY)
data class StripeBottomSheetLayoutInfo(
    val sheetShape: Shape,
    val sheetBackgroundColor: Color,
    val scrimColor: Color,
)

@Composable
fun rememberStripeBottomSheetLayoutInfo(
    cornerRadius: Dp = MaterialTheme.stripeShapes.bottomSheetCornerRadius.dp,
    sheetBackgroundColor: Color = MaterialTheme.colors.surface,
    scrimColor: Color = ModalBottomSheetDefaults.scrimColor,
): StripeBottomSheetLayoutInfo {
    return remember {
        StripeBottomSheetLayoutInfo(
            sheetShape = sheetShape(cornerRadius),
            sheetBackgroundColor = sheetBackgroundColor,
            scrimColor = scrimColor,
        )
    }
}

private fun sheetShape(cornerRadius: Dp): RoundedCornerShape {
    return RoundedCornerShape(
        topStart = cornerRadius,
        topEnd = cornerRadius,
    )
}
