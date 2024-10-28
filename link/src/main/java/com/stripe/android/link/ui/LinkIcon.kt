package com.stripe.android.link.ui

import androidx.compose.material.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import com.stripe.android.uicore.R

@Composable
fun LinkIcon(
    modifier: Modifier = Modifier,
    tint: Color = Color.Unspecified
) {
    Icon(
        painter = painterResource(R.drawable.stripe_link_logo_bw),
        contentDescription = stringResource(com.stripe.android.R.string.stripe_link),
        modifier = modifier,
        tint = tint,
    )
}
