package com.stripe.android.paymentsheet.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.stripe.android.paymentsheet.R
import com.stripe.android.ui.core.PaymentsTheme

@Preview
@Composable
internal fun AddressOptionsAppBar() {
    PaymentsTheme {
        Surface {
            AddressOptionsAppBar(
                isRootScreen = true,
                onButtonClick = {}
            )
        }
    }
}

@Composable
fun AddressOptionsAppBar(
    isRootScreen: Boolean,
    onButtonClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        IconButton(
            onClick = onButtonClick,
        ) {
            Icon(
                painter = painterResource(
                    id = if (isRootScreen) {
                        R.drawable.stripe_ic_paymentsheet_close_enabled
                    } else {
                        R.drawable.stripe_ic_paymentsheet_back_enabled
                    }
                ),
                contentDescription = stringResource(
                    if (isRootScreen) R.string.stripe_paymentsheet_close
                    else R.string.stripe_paymentsheet_back
                ),
                tint = MaterialTheme.colors.onSurface
            )
        }
    }
}