package com.stripe.android.customersheet.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.ModalBottomSheetLayout
import androidx.compose.material.ModalBottomSheetValue
import androidx.compose.material.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterialApi::class)
@Composable
internal fun CustomerBottomSheet(
    modifier: Modifier = Modifier,
    onClose: () -> Unit = {},
    sheetContent: @Composable ColumnScope.() -> Unit,
) {
    val coroutineScope = rememberCoroutineScope()
    val modalSheetState = rememberModalBottomSheetState(
        initialValue = ModalBottomSheetValue.Hidden,
        confirmValueChange = {
            if (it ==  ModalBottomSheetValue.Hidden) {
                onClose()
            }
            it != ModalBottomSheetValue.Expanded
        },
        skipHalfExpanded = true
    )

    LaunchedEffect(Unit) {
        modalSheetState.show()
    }

    BackHandler(modalSheetState.isVisible) {
        coroutineScope.launch {
            modalSheetState.hide()
            onClose()
        }
    }

    ModalBottomSheetLayout(
        sheetState = modalSheetState,
        sheetContent = sheetContent,
        modifier = modifier
    ) { }
}
