package com.stripe.android.common.taptoadd

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.stripe.android.paymentsheet.R
import com.stripe.android.uicore.StripeTheme
import com.stripe.android.uicore.getBorderStroke

@Composable
internal fun TapToAddButton(
    enabled: Boolean,
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(
        StripeTheme.primaryButtonStyle.shape.cornerRadius.dp
    )

    TextButton(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .defaultMinSize(
                minHeight = StripeTheme.primaryButtonStyle.shape.height.dp
            ),
        enabled = enabled,
        shape = shape,
        border = MaterialTheme.getBorderStroke(isSelected = false),
        colors = ButtonDefaults.buttonColors(
            backgroundColor = MaterialTheme.colors.surface,
            disabledBackgroundColor = MaterialTheme.colors.surface,
        )
    ) {
        Text(
            text = stringResource(R.string.stripe_paymentsheet_tap_to_add),
            color = MaterialTheme.colors.onSurface,
            style = MaterialTheme.typography.body1.copy(fontWeight = FontWeight.Medium),
            modifier = Modifier.align(Alignment.CenterVertically)
        )
    }
}

@Preview
@Composable
private fun TestTapToAddButton() {
    StripeTheme {
        Box(
            Modifier
                .background(MaterialTheme.colors.surface)
                .padding(12.dp)
        ) {
            TapToAddButton(enabled = true) {}
        }
    }
}
