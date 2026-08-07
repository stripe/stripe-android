package com.stripe.android.common.ui

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.stripe.android.uicore.FormScrollContext
import com.stripe.android.uicore.LocalFormScrollContext
import com.stripe.android.uicore.StripeTheme

@Composable
internal fun BottomSheetScaffold(
    topBar: @Composable () -> Unit,
    content: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    scrollState: ScrollState = rememberScrollState()
) {
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

        // We provide the IME padding before the vertical scroll modifier to make sure that the
        // content moves up correctly if it's covered by the keyboard when it's being focused.
        val viewportTopYState = remember { mutableIntStateOf(0) }
        val scrollContext = remember(scrollState) { FormScrollContext(scrollState, viewportTopYState) }
        CompositionLocalProvider(LocalFormScrollContext provides scrollContext) {
            Column(
                modifier = Modifier
                    .imePadding()
                    .onGloballyPositioned { viewportTopYState.intValue = it.positionInRoot().y.toInt() }
                    .verticalScroll(scrollState)
            ) {
                Spacer(Modifier.height(StripeTheme.formInsets.top.dp))
                content()
                Spacer(modifier = Modifier.windowInsetsBottomHeight(WindowInsets.navigationBars))
            }
        }
    }
}
