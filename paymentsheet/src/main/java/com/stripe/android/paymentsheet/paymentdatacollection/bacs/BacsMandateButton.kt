package com.stripe.android.paymentsheet.paymentdatacollection.bacs

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp

@Composable
internal fun BacsMandateButton(type: BacsMandateButtonType, label: String, onClick: () -> Unit) {
    val colors = when (type) {
        BacsMandateButtonType.Primary -> ButtonDefaults.buttonColors(
            backgroundColor = MaterialTheme.colors.primary,
            contentColor = MaterialTheme.colors.onPrimary
        )
        BacsMandateButtonType.Secondary -> ButtonDefaults.buttonColors(
            backgroundColor = MaterialTheme.colors.secondary,
            contentColor = MaterialTheme.colors.onSecondary
        )
    }

    return TextButton(
        modifier = Modifier
            .fillMaxWidth()
            .height(44.dp)
            .addButtonBorder(
                type,
                MaterialTheme.shapes.small,
                MaterialTheme.colors.secondaryVariant
            ),
        shape = MaterialTheme.shapes.small,
        colors = colors,
        onClick = onClick
    ) {
        Text(text = label)
    }
}

private fun Modifier.addButtonBorder(
    type: BacsMandateButtonType,
    shape: Shape,
    color: Color
): Modifier {
    return when (type) {
        BacsMandateButtonType.Primary -> this
        BacsMandateButtonType.Secondary -> border(
            border = BorderStroke(
                width = 1.5.dp,
                color = color,
            ),
            shape = shape
        )
    }
}
