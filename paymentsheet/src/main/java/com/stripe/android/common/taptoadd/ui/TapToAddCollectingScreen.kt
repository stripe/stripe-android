package com.stripe.android.common.taptoadd.ui

import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import com.stripe.android.common.ui.LoadingIndicator

@Composable
internal fun TapToAddCollectingScreen() {
    LoadingIndicator(
        color = MaterialTheme.colors.primary,
    )
}
