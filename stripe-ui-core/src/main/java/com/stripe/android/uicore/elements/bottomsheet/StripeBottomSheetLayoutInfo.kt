package com.stripe.android.uicore.elements.bottomsheet

import androidx.annotation.RestrictTo
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun rememberStripeBottomSheetLayoutInfo(
    cornerRadius: Dp = MaterialTheme.stripeShapes.bottomSheetCornerRadius.dp,
    sheetBackgroundColor: Color = MaterialTheme.colorScheme.surface,
    scrimColor: Color = BottomSheetDefaults.ScrimColor,
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
