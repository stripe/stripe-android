package com.stripe.android.link.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.ModalBottomSheetValue
import androidx.compose.material.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import com.stripe.android.link.LinkActivityViewModel
import com.stripe.android.paymentsheet.utils.EventReporterProvider
import com.stripe.android.uicore.utils.collectAsState
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterialApi::class)
@Composable
internal fun FullScreenContent(
    modifier: Modifier,
    viewModel: LinkActivityViewModel,
    onBackPressed: () -> Unit
) {
    var bottomSheetContent by remember { mutableStateOf<BottomSheetContent?>(null) }
    val sheetState = rememberModalBottomSheetState(ModalBottomSheetValue.Hidden)
    val coroutineScope = rememberCoroutineScope()
    val appBarState by viewModel.linkAppBarState.collectAsState()
    val navController = rememberNavController()

    if (bottomSheetContent != null) {
        DisposableEffect(bottomSheetContent) {
            coroutineScope.launch { sheetState.show() }
            onDispose {
                coroutineScope.launch { sheetState.hide() }
            }
        }
    }

    Box(
        modifier = modifier
    ) {
        EventReporterProvider(
            eventReporter = viewModel.eventReporter
        ) {
            LinkContent(
                viewModel = viewModel,
                navController = navController,
                appBarState = appBarState,
                sheetState = sheetState,
                bottomSheetContent = bottomSheetContent,
                onUpdateSheetContent = {
                    bottomSheetContent = it
                },
                onBackPressed = onBackPressed,
                moveToWeb = viewModel::moveToWeb
            )
        }
    }

    LaunchedEffect(Unit) {
        viewModel.navController = navController
        viewModel.linkScreenScreenCreated()
    }
}
