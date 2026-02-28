package com.stripe.android.paymentsheet.example.playground.logger

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.ModalBottomSheetLayout
import androidx.compose.material.ModalBottomSheetValue
import androidx.compose.material.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.stripe.android.uicore.utils.collectAsState
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun LoggerOverlay(
    content: @Composable () -> Unit,
) {
    val viewModel: LoggerModalViewModel = viewModel()
    val viewState by viewModel.viewState.collectAsState()
    val sheetState = rememberModalBottomSheetState(
        initialValue = ModalBottomSheetValue.Hidden,
        skipHalfExpanded = true,
    )
    val hapticFeedback = LocalHapticFeedback.current
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect("logger_effects") {
        viewModel.vibrateEffect.conflate().collect { _ ->
            hapticFeedback.performHapticFeedback(HapticFeedbackType.SegmentTick)
            delay(500.milliseconds)
        }
    }

    ModalBottomSheetLayout(
        sheetState = sheetState,
        sheetShape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
        sheetContent = {
            LoggerBottomSheetContent(
                viewState = viewState,
                onTagFilterChanged = viewModel::onTagFilterChanged,
                onMessageFilterChanged = viewModel::onMessageFilterChanged,
            )
        },
    ) {
        Box(Modifier.fillMaxSize()) {
            content()
            LoggerBubble(
                logCount = viewState.logs.size,
                onClick = { coroutineScope.launch { sheetState.show() } },
            )
        }
    }
}
