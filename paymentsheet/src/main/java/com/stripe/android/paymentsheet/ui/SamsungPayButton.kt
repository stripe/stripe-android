package com.stripe.android.paymentsheet.ui

import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.stripe.android.uicore.StripeTheme

private val SamsungPayBackgroundColor = Color(0xFF1428A0)

@Composable
internal fun SamsungPayButton(
    isEnabled: Boolean,
    modifier: Modifier = Modifier,
    onPressed: () -> Unit,
) {
    Button(
        onClick = onPressed,
        modifier = modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = PrimaryButtonTheme.shape.height)
            .semantics {
                contentDescription = "Samsung Pay"
            },
        enabled = isEnabled,
        shape = RoundedCornerShape(
            StripeTheme.primaryButtonStyle.shape.cornerRadius.dp
        ),
        elevation = ButtonDefaults.elevation(0.dp, 0.dp, 0.dp, 0.dp, 0.dp),
        colors = ButtonDefaults.buttonColors(
            backgroundColor = SamsungPayBackgroundColor,
            disabledBackgroundColor = SamsungPayBackgroundColor.copy(alpha = 0.5f),
        ),
    ) {
        Text(
            text = "Samsung Pay",
            color = Color.White,
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium,
        )
    }
}
