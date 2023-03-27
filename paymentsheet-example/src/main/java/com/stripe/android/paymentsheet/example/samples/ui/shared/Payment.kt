package com.stripe.android.paymentsheet.example.samples.ui

import android.graphics.drawable.Drawable
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.stripe.android.paymentsheet.example.R
import com.stripe.android.paymentsheet.example.utils.rememberDrawablePainter

@Composable
fun PaymentMethodSelector(
    isEnabled: Boolean,
    paymentMethodLabel: String,
    paymentMethodIcon: Drawable?,
    onClick: () -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = stringResource(R.string.payment_method),
            modifier = Modifier
                .weight(1f)
                .padding(vertical = PADDING),
            fontSize = MAIN_FONT_SIZE
        )

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.End,
            modifier = Modifier.clickable(
                enabled = isEnabled,
                onClick = onClick
            ),
        ) {
            if (paymentMethodIcon != null) {
                Icon(
                    painter = rememberDrawablePainter(paymentMethodIcon),
                    contentDescription = null, // decorative element
                    modifier = Modifier.padding(horizontal = 4.dp),
                    tint = Color.Unspecified
                )

                Text(
                    text = paymentMethodLabel,
                    fontSize = MAIN_FONT_SIZE,
                    color = if (isEnabled) {
                        Color.Unspecified
                    } else {
                        Color.Gray
                    }
                )
            } else {
                TextButton(
                    onClick = onClick,
                    enabled = isEnabled,
                ) {
                    Text(text = paymentMethodLabel)
                }
            }
        }
    }
}

@Composable
fun BuyButton(
    buyButtonEnabled: Boolean,
    onClick: () -> Unit,
) {
    TextButton(
        enabled = buyButtonEnabled,
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 4.dp),
        onClick = onClick,
        colors = ButtonDefaults.textButtonColors(
            backgroundColor = BUTTON_COLOR,
            contentColor = Color.White
        )
    ) {
        Text(
            stringResource(R.string.buy),
            modifier = Modifier.padding(vertical = 2.dp),
            fontSize = MAIN_FONT_SIZE
        )
    }
}
