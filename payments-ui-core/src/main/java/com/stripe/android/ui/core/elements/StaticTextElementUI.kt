package com.stripe.android.ui.core.elements

import androidx.annotation.RestrictTo
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
fun StaticElementUI(
    element: StaticTextElement
) {
    Text(
        stringResource(element.stringResId, element.merchantName ?: ""),
        fontSize = element.fontSizeSp.sp,
        letterSpacing = element.letterSpacingSp.sp,
        modifier = Modifier
            .padding(vertical = 8.dp)
            .semantics(mergeDescendants = true) {}, // makes it a separate accessibile item
        color = when {
            element.color != null -> {
                colorResource(element.color)
            }
            isSystemInDarkTheme() -> {
                Color.LightGray
            }
            else -> {
                Color.Black
            }
        }
    )
}
