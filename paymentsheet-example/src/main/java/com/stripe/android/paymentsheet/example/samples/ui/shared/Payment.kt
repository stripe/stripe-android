package com.stripe.android.paymentsheet.example.samples.ui.shared

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTag
import androidx.compose.ui.semantics.text
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import com.stripe.android.paymentsheet.example.R
import com.stripe.android.paymentsheet.example.samples.ui.MAIN_FONT_SIZE
import com.stripe.android.paymentsheet.example.samples.ui.PADDING

@Composable
fun PaymentMethodSelector(
    isEnabled: Boolean,
    paymentMethodLabel: String,
    paymentMethodPainter: Painter?,
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
            ).semantics {
                testTag = PAYMENT_METHOD_SELECTOR_TEST_TAG
                text = AnnotatedString(paymentMethodLabel)
            },
        ) {
            if (paymentMethodPainter != null) {
                Icon(
                    painter = paymentMethodPainter,
                    contentDescription = null, // decorative element
                    modifier = Modifier.padding(horizontal = 4.dp),
                    tint = Color.Unspecified,
                )

                Text(
                    text = paymentMethodLabel,
                    fontSize = MAIN_FONT_SIZE,
                    modifier = Modifier.alpha(if (isEnabled) 1f else 0.5f),
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
            .padding(top = 4.dp)
            .testTag(CHECKOUT_TEST_TAG),
        onClick = onClick,
        colors = ButtonDefaults.textButtonColors(
            backgroundColor = MaterialTheme.colors.primary,
            contentColor = MaterialTheme.colors.onPrimary,
        )
    ) {
        Text(
            stringResource(R.string.buy),
            modifier = Modifier.padding(vertical = 2.dp),
            fontSize = MAIN_FONT_SIZE,
        )
    }
}

const val PAYMENT_METHOD_SELECTOR_TEST_TAG = "PAYMENT_METHOD_SELECTOR"
const val CHECKOUT_TEST_TAG = "CHECKOUT"
