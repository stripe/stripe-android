package com.stripe.android.paymentsheet.ui

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.stripe.android.paymentsheet.R
import com.stripe.android.uicore.stripeColors
import com.stripe.android.ui.core.R as StripeUiCoreR

@Composable
internal fun AddressOptionsAppBar(
    isRootScreen: Boolean,
    onButtonClick: () -> Unit
) {
    TopAppBar(
        modifier = Modifier.fillMaxWidth(),
        elevation = 0.dp,
        backgroundColor = MaterialTheme.colors.surface
    ) {
        IconButton(
            onClick = onButtonClick
        ) {
            Icon(
                painter = painterResource(
                    id = if (isRootScreen) {
                        R.drawable.stripe_ic_paymentsheet_close
                    } else {
                        R.drawable.stripe_ic_paymentsheet_back
                    }
                ),
                contentDescription = stringResource(
                    if (isRootScreen) {
                        R.string.stripe_paymentsheet_close
                    } else {
                        StripeUiCoreR.string.stripe_back
                    }
                ),
                tint = MaterialTheme.stripeColors.appBarIcon
            )
        }
    }
}
