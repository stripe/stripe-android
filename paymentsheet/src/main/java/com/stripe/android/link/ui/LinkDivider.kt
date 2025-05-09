package com.stripe.android.link.ui

import androidx.compose.material.Divider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.stripe.android.link.theme.LinkTheme

@Composable
internal fun LinkDivider(modifier: Modifier = Modifier) {
    Divider(
        modifier = modifier,
        thickness = 0.5.dp,
        color = LinkTheme.colors.borderDefault,
    )
}
