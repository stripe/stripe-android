package com.stripe.android.common.taptoadd.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

@Composable
internal fun TapToAddCardLayout(
    content: @Composable () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(
            top = LocalTapToAddMaxContentHeight.current * 0.08f
        ),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        content()
    }
}
