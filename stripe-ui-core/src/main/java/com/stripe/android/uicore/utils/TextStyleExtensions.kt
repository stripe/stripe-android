package com.stripe.android.uicore.utils

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.LayoutDirection
import com.stripe.android.uicore.elements.InputController

@Composable
internal fun TextStyle.withLtrDirectionEnforcedIfNeeded(
    controller: InputController,
): TextStyle {
    val layoutDirection = LocalLayoutDirection.current

    return if (controller.enforceLeftToRightTextDirection) {
        copy(
            textDirection = TextDirection.Ltr,
            textAlign = if (layoutDirection == LayoutDirection.Rtl && textAlign == TextAlign.Unspecified) {
                TextAlign.End
            } else {
                textAlign
            }
        )
    } else {
        this
    }
}
