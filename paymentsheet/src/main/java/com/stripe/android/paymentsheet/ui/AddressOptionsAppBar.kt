package com.stripe.android.paymentsheet.ui

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import com.stripe.android.paymentsheet.R
import com.stripe.android.uicore.stripeColorScheme
import com.stripe.android.ui.core.R as StripeUiCoreR

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun AddressOptionsAppBar(
    isRootScreen: Boolean,
    onButtonClick: () -> Unit
) {
    TopAppBar(
        modifier = Modifier.fillMaxWidth(),
        title = {},
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        navigationIcon = {
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
                    tint = MaterialTheme.stripeColorScheme.appBarIcon
                )
            }
        }
    )
}
