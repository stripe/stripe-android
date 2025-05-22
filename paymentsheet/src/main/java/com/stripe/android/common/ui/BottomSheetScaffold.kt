package com.stripe.android.common.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex

@Composable
internal fun BottomSheetScaffold(
    topBar: @Composable () -> Unit,
    content: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    scrollState: ScrollState = rememberScrollState(),
    hideWithoutDismissing: Boolean = false,
) {
    AnimatedContent(
        modifier = modifier,
        targetState = hideWithoutDismissing,
        transitionSpec = {
            val animationSpec = tween<IntSize>(durationMillis = 300, easing = FastOutSlowInEasing)
            expandVertically(expandFrom = Alignment.Top, animationSpec = animationSpec) togetherWith
                shrinkVertically(shrinkTowards = Alignment.Top, animationSpec = animationSpec)
        },
    ) { isHidden ->
        if (isHidden) {
            HiddenBottomSheetPlaceholder()
        } else {
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
                Column(
                    modifier = Modifier
                        .imePadding()
                        .verticalScroll(scrollState)
                ) {
                    content()
                    Spacer(modifier = Modifier.windowInsetsBottomHeight(WindowInsets.navigationBars))
                }
            }
        }
    }
}

@Composable
private fun HiddenBottomSheetPlaceholder() {
    Box(
        Modifier
            .height(1.dp)
            .fillMaxWidth()
    )
}
