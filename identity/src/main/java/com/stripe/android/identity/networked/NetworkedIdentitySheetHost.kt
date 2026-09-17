package com.stripe.android.identity.networked

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.ModalBottomSheetLayout
import androidx.compose.material.ModalBottomSheetValue
import androidx.compose.material.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.stripe.android.uicore.utils.collectAsState

/**
 * Shows the Link sheet over [content] while a Networked Identity attempt is in progress. Without a
 * [viewModel] (Networked Identity unavailable) only [content] is shown.
 */
@OptIn(ExperimentalMaterialApi::class)
@Composable
internal fun NetworkedIdentitySheetHost(
    viewModel: NetworkedIdentityViewModel?,
    savedItems: List<String>,
    content: @Composable () -> Unit,
) {
    if (viewModel == null) {
        content()
        return
    }
    val state by viewModel.state.collectAsState()
    val mode by viewModel.mode.collectAsState()
    val actions = remember(viewModel) { viewModel.screenActions() }
    val sheetState = rememberModalBottomSheetState(
        initialValue = ModalBottomSheetValue.Hidden,
        skipHalfExpanded = true,
    )

    LaunchedEffect(state.isSheetVisible) {
        if (state.isSheetVisible) sheetState.show() else sheetState.hide()
    }
    LaunchedEffect(sheetState.isVisible) {
        // Swiping the sheet away or tapping outside it cancels the attempt.
        if (!sheetState.isVisible && viewModel.state.value.isSheetVisible) viewModel.cancel()
    }

    ModalBottomSheetLayout(
        sheetState = sheetState,
        sheetShape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
        sheetContent = {
            if (state.isSheetVisible) {
                NetworkedIdentityScreen(state = state, mode = mode, savedItems = savedItems, actions = actions)
            } else {
                // The sheet needs content with a size even while hidden.
                Spacer(Modifier.height(1.dp))
            }
        },
        content = content,
    )
}
