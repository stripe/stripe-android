package com.stripe.android.common.ui

import android.os.Build
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.material.ModalBottomSheetValue.Expanded
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.stripe.android.paymentsheet.BuildConfig
import com.stripe.android.uicore.elements.bottomsheet.StripeBottomSheetLayout
import com.stripe.android.uicore.elements.bottomsheet.StripeBottomSheetState
import com.stripe.android.uicore.elements.bottomsheet.rememberStripeBottomSheetLayoutInfo
import com.stripe.android.utils.rememberActivityOrNull

@Composable
internal fun ElementsBottomSheetLayout(
    state: StripeBottomSheetState,
    modifier: Modifier = Modifier,
    onDismissed: () -> Unit,
    content: @Composable () -> Unit,
) {
    val activity = rememberActivityOrNull() as? ComponentActivity
    val layoutInfo = rememberStripeBottomSheetLayoutInfo(
        scrimColor = Color.Black.copy(alpha = 0.32f),
    )

    LaunchedEffect(Unit) {
        state.skipHideAnimation = skipHideAnimation
    }

    val isExpanded = state.modalBottomSheetState.targetValue == Expanded

    val statusBarColorAlpha by animateFloatAsState(
        targetValue = if (isExpanded) layoutInfo.scrimColor.alpha else 0f,
        animationSpec = tween(),
        label = "StatusBarColorAlpha",
    )

    LaunchedEffect(statusBarColorAlpha) {
        activity?.enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.auto(
                lightScrim = layoutInfo.scrimColor.copy(statusBarColorAlpha).toArgb(),
                darkScrim = layoutInfo.scrimColor.copy(statusBarColorAlpha).toArgb()
            ),
            navigationBarStyle = SystemBarStyle.auto(
                lightScrim = Color.Transparent.toArgb(),
                darkScrim = Color.Transparent.toArgb()
            )
        )
    }

    StripeBottomSheetLayout(
        state = state,
        layoutInfo = layoutInfo,
        modifier = modifier,
        onDismissed = onDismissed,
        sheetContent = content,
    )
}

private val skipHideAnimation: Boolean
    get() = BuildConfig.DEBUG && (isRunningUnitTest || isRunningUiTest)

private val isRunningUnitTest: Boolean
    get() {
        return runCatching {
            Build.FINGERPRINT.lowercase() == "robolectric"
        }.getOrDefault(false)
    }

private val isRunningUiTest: Boolean
    get() {
        return runCatching {
            Class.forName("androidx.test.InstrumentationRegistry")
        }.isSuccess
    }
