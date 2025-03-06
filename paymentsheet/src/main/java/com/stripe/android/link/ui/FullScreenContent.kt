package com.stripe.android.link.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.ModalBottomSheetValue
import androidx.compose.material.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavBackStackEntry
import androidx.navigation.compose.rememberNavController
import com.stripe.android.link.LinkAction
import com.stripe.android.link.LinkActivityResult
import com.stripe.android.link.model.LinkAccount
import com.stripe.android.paymentsheet.analytics.EventReporter
import com.stripe.android.paymentsheet.utils.EventReporterProvider
import com.stripe.android.uicore.navigation.NavigationEffects
import com.stripe.android.uicore.navigation.NavigationIntent
import com.stripe.android.uicore.navigation.rememberKeyboardController
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterialApi::class)
@Composable
internal fun FullScreenContent(
    modifier: Modifier,
    appBarState: LinkAppBarState,
    eventReporter: EventReporter,
    navigationEvents: SharedFlow<NavigationIntent>,
    onBackPressed: () -> Unit,
    moveToWeb: () -> Unit,
    handleViewAction: (LinkAction) -> Unit,
    dismissWithResult: ((LinkActivityResult) -> Unit)?,
    getLinkAccount: () -> LinkAccount?,
    onNavBackStackEntryChanged: (NavBackStackEntry?) -> Unit,
) {
    var bottomSheetContent by remember { mutableStateOf<BottomSheetContent?>(null) }
    val sheetState = rememberModalBottomSheetState(ModalBottomSheetValue.Hidden)
    val coroutineScope = rememberCoroutineScope()
    val navController = rememberNavController()
    val keyboardController = rememberKeyboardController()

    if (bottomSheetContent != null) {
        DisposableEffect(bottomSheetContent) {
            coroutineScope.launch { sheetState.show() }
            onDispose {
                coroutineScope.launch { sheetState.hide() }
            }
        }
    }

    BackHandler {
        if (!navController.popBackStack()) {
            onBackPressed()
        }
    }

    NavigationEffects(
        navigationChannel = navigationEvents,
        navHostController = navController,
        keyboardController = keyboardController,
        onBackStackEntryUpdated = onNavBackStackEntryChanged
    )

    Box(
        modifier = modifier
    ) {
        EventReporterProvider(
            eventReporter = eventReporter
        ) {
            LinkContent(
                navController = navController,
                appBarState = appBarState,
                sheetState = sheetState,
                bottomSheetContent = bottomSheetContent,
                onUpdateSheetContent = {
                    bottomSheetContent = it
                },
                moveToWeb = moveToWeb,
                handleViewAction = handleViewAction,
                dismissWithResult = dismissWithResult,
                getLinkAccount = getLinkAccount,
            )
        }
    }
}
