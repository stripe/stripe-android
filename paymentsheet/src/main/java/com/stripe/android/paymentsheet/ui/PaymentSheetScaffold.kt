package com.stripe.android.paymentsheet.ui

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex

@Composable
internal fun PaymentSheetScaffold(
    topBar: @Composable () -> Unit,
    content: @Composable (Modifier) -> Unit,
    modifier: Modifier = Modifier,
) {
    val scrollState = rememberScrollState()

    val targetElevation by remember {
        derivedStateOf {
            if (scrollState.value > 0) {
                8.dp
            } else {
                0.dp
            }
        }
    }

    val elevation by animateDpAsState(
        targetValue = targetElevation,
        label = "PaymentSheetTopBarElevation",
    )

    Column(modifier = modifier) {
        // We need to set a z-index to make sure that the Surface's elevation shadow is rendered
        // correctly above the screen content.
        Surface(elevation = elevation, modifier = Modifier.zIndex(1f)) {
            topBar()
        }

        content(
            // We provide the IME padding before the vertical scroll modifier to make sure that the
            // content moves up correctly if it's covered by the keyboard when it's being focused.
            Modifier
                .imePadding()
                .verticalScroll(scrollState)
        )
    }
}
