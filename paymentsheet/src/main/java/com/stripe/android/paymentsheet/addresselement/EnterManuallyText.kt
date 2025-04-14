package com.stripe.android.paymentsheet.addresselement

import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withLink
import com.stripe.android.paymentsheet.R
import com.stripe.android.uicore.StripeThemeDefaults

@Composable
internal fun EnterManuallyText(
    onClick: () -> Unit
) {
    Text(
        text = buildAnnotatedString {
            val link =
                LinkAnnotation.Clickable(
                    tag = "EnterAddressManually",
                    linkInteractionListener = {
                        onClick()
                    }
                )

            withLink(link) {
                append(
                    stringResource(
                        id = R.string.stripe_paymentsheet_enter_address_manually
                    )
                )
            }
        },
        style = MaterialTheme.typography.body1.copy(
            fontSize = StripeThemeDefaults.typography.largeFontSize,
            color = MaterialTheme.colors.primary
        )
    )
}
