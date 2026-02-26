package com.stripe.android.common.taptoadd.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.stripe.android.common.ui.LoadingIndicator

@Composable
internal fun TapToAddCollectingScreen() {
    Box(Modifier.fillMaxSize()) {
        LoadingIndicator(
            modifier = Modifier.align(Alignment.Center),
            color = MaterialTheme.colorScheme.primary,
        )
    }
}
