package com.stripe.android.connect.example.ui.common

import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import com.stripe.android.connect.example.R
import com.stripe.android.uicore.R as StripeUiCoreR

@Composable
fun BackIconButton(onClick: () -> Unit) {
    IconButton(onClick = onClick) {
        Icon(
            painter = painterResource(StripeUiCoreR.drawable.stripe_ic_material_close),
            contentDescription = stringResource(R.string.back)
        )
    }
}

@Composable
fun CustomizeAppearanceIconButton(
    onClick: () -> Unit,
) {
    IconButton(onClick = onClick) {
        Icon(
            painter = painterResource(R.drawable.ic_material_palette),
            contentDescription = stringResource(R.string.customize_appearance),
        )
    }
}
