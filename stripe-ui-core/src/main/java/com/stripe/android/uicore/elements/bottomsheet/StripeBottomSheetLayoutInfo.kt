package com.stripe.android.uicore.elements.bottomsheet

import android.os.Build
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
    cornerRadius: Dp = MaterialTheme.stripeShapes.cornerRadius.dp,
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
    /*
     * Following Compose 1.7.0, Compose UI tests using Robolectric & `performClick` inside of a Modal Bottom Sheet
     * context broke. `performClick` wouldn't perform the requested action.
     *
     * According to https://github.com/robolectric/robolectric/issues/9595, it seems `performClick` is broken when
     * clicking within a component with a `clip` modifier when testing with API versions excluding API 24, 25, 26, and
     * 28 (clicking works fine in production). Modal Bottom Sheet uses the `clip` modifier internally when creating
     * the sheet surface.
     *
     * Oddly the issue only occurs when passing a non-uniform shape. Passing a shape with uniform corners does not
     * cause the above issue to occur.
     *
     * If this is solved by the Robolectric or Compose team, we should remove this check below.
     */
    return if (Build.FINGERPRINT.lowercase() == "robolectric") {
        RoundedCornerShape(cornerRadius)
    } else {
        RoundedCornerShape(
            topStart = cornerRadius,
            topEnd = cornerRadius,
        )
    }
}
