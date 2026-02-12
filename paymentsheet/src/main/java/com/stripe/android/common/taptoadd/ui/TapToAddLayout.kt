package com.stripe.android.common.taptoadd.ui

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.stripe.android.paymentsheet.R

@Composable
internal fun TapToAddLayout(
    screen: TapToAddNavigator.Screen,
    onCancel: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colors.background)
            .windowInsetsPadding(WindowInsets.systemBars)
    ) {
        Crossfade(screen) { screen ->
            Column(Modifier.fillMaxSize()) {
                Box(Modifier.padding(20.dp).weight(1f)) {
                    screen.Content()
                }

                CancelButton(
                    button = screen.cancelButton,
                    onClick = onCancel,
                )
            }
        }
    }
}

@Composable
private fun ColumnScope.CancelButton(
    button: TapToAddNavigator.CancelButton,
    onClick: () -> Unit,
) {
    val modifier = Modifier
        .padding(
            top = 20.dp,
            bottom = 50.dp,
            start = 20.dp,
            end = 20.dp,
        )
        .align(Alignment.CenterHorizontally)

    when (button) {
        TapToAddNavigator.CancelButton.None -> Unit
        TapToAddNavigator.CancelButton.Invisible -> {
            Spacer(modifier.size(40.dp))
        }
        TapToAddNavigator.CancelButton.Visible -> {
            Image(
                painter = painterResource(R.drawable.stripe_ic_paymentsheet_tta_close),
                contentDescription = stringResource(com.stripe.android.R.string.stripe_close),
                modifier = modifier
                    .size(40.dp)
                    .clickable(
                        enabled = true,
                        role = Role.Button,
                        onClick = onClick,
                    )
            )
        }
    }
}
