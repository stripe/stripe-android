package com.stripe.android.paymentsheet.addresselement

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.ContentAlpha
import androidx.compose.material.LocalContentAlpha
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.stripe.android.paymentsheet.R
import com.stripe.android.ui.core.PaymentsTheme
import com.stripe.android.ui.core.getBackgroundColor
import com.stripe.android.ui.core.getOnBackgroundColor

@Composable
internal fun AddressElementPrimaryButton(
    isEnabled: Boolean,
    onButtonClick: () -> Unit
) {
    val context = LocalContext.current
    val background = Color(PaymentsTheme.primaryButtonStyle.getBackgroundColor(context))
    val onBackground = Color(PaymentsTheme.primaryButtonStyle.getOnBackgroundColor(context))
    CompositionLocalProvider(
        LocalContentAlpha provides if (isEnabled) ContentAlpha.high else ContentAlpha.disabled
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp),
            contentAlignment = Alignment.Center
        ) {
            TextButton(
                onClick = onButtonClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp),
                enabled = isEnabled,
                shape = MaterialTheme.shapes.medium,
                colors = ButtonDefaults.buttonColors(
                    backgroundColor = background,
                    disabledBackgroundColor = background
                )
            ) {
                Text(
                    text = stringResource(
                        R.string.stripe_paymentsheet_address_element_primary_button
                    ),
                    color = onBackground.copy(alpha = LocalContentAlpha.current)
                )
            }
        }
    }
}
