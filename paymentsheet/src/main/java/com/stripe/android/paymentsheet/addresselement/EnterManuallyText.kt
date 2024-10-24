package com.stripe.android.paymentsheet.addresselement

import androidx.compose.foundation.text.ClickableText
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.buildAnnotatedString
import com.stripe.android.paymentsheet.R
import com.stripe.android.uicore.StripeThemeDefaults

@Composable
internal fun EnterManuallyText(
    onClick: () -> Unit
) {
    @Suppress("DEPRECATION")
    ClickableText(
        text = buildAnnotatedString {
            append(
                stringResource(
                    id = R.string.stripe_paymentsheet_enter_address_manually
                )
            )
        },
        style = MaterialTheme.typography.body1.copy(
            fontSize = StripeThemeDefaults.typography.largeFontSize,
            color = MaterialTheme.colors.primary
        )
    ) {
        onClick()
    }
}
