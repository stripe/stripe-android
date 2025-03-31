package com.stripe.android.paymentsheet.ui

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.ZeroCornerSize
import androidx.compose.material.ContentAlpha
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import com.stripe.android.uicore.elements.TextFieldColors
import com.stripe.android.uicore.stripeColors

@Composable
internal fun CommonTextField(
    value: String,
    label: String,
    modifier: Modifier = Modifier,
    trailingIcon: @Composable (() -> Unit)? = null,
    shouldShowError: Boolean = false,
    shape: Shape =
        MaterialTheme.shapes.small.copy(bottomEnd = ZeroCornerSize, bottomStart = ZeroCornerSize),
) {
    TextField(
        modifier = modifier.fillMaxWidth(),
        value = value,
        enabled = false,
        label = {
            Label(
                text = label,
            )
        },
        trailingIcon = trailingIcon,
        shape = shape,
        colors = TextFieldColors(
            shouldShowError = shouldShowError,
        ),
        onValueChange = {},
    )
}

@Composable
private fun Label(
    text: String,
) {
    Text(
        text = text,
        color = MaterialTheme.stripeColors.placeholderText.copy(alpha = ContentAlpha.disabled),
        style = MaterialTheme.typography.subtitle1
    )
}
