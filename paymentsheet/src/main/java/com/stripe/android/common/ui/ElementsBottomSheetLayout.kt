package com.stripe.android.common.ui

import android.os.Build
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.ModalBottomSheetValue.Expanded
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import com.stripe.android.paymentsheet.BuildConfig
import com.stripe.android.uicore.elements.bottomsheet.StripeBottomSheetLayout
import com.stripe.android.uicore.elements.bottomsheet.StripeBottomSheetState
import com.stripe.android.uicore.elements.bottomsheet.rememberStripeBottomSheetLayoutInfo

@OptIn(ExperimentalMaterialApi::class)
@Composable
internal fun ElementsBottomSheetLayout(
    state: StripeBottomSheetState,
    modifier: Modifier = Modifier,
    onDismissed: () -> Unit,
    content: @Composable () -> Unit,
) {
    @Suppress("DEPRECATION")
    val systemUiController = rememberSystemUiController()
    val layoutInfo = rememberStripeBottomSheetLayoutInfo()

    LaunchedEffect(Unit) {
        state.skipHideAnimation = skipHideAnimation
    }

    val isExpanded = state.modalBottomSheetState.targetValue == Expanded

    val statusBarColorAlpha by animateFloatAsState(
        targetValue = if (isExpanded) layoutInfo.scrimColor.alpha else 0f,
        animationSpec = tween(),
        label = "StatusBarColorAlpha",
    )

    LaunchedEffect(systemUiController, statusBarColorAlpha) {
        systemUiController.setStatusBarColor(
            color = layoutInfo.scrimColor.copy(statusBarColorAlpha),
            darkIcons = false,
        )
    }

    LaunchedEffect(systemUiController) {
        systemUiController.setNavigationBarColor(
            color = Color.Transparent,
            darkIcons = false,
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
