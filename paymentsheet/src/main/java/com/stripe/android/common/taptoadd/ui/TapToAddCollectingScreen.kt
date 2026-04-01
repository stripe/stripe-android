package com.stripe.android.common.taptoadd.ui

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.stripe.android.common.ui.LoadingIndicator

@Composable
internal fun ColumnScope.TapToAddCollectingScreen() {
    Spacer(Modifier.weight(1f))

    LoadingIndicator(
        modifier = Modifier.align(Alignment.CenterHorizontally),
        color = MaterialTheme.colors.primary,
    )

    Spacer(Modifier.weight(1f))
}
