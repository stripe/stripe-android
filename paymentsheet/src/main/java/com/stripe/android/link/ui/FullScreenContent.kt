package com.stripe.android.link.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.ModalBottomSheetValue
import androidx.compose.material.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.stripe.android.link.LinkAction
import com.stripe.android.link.LinkActivityResult
import com.stripe.android.link.LinkScreen
import com.stripe.android.link.model.LinkAccount
import com.stripe.android.paymentsheet.analytics.EventReporter
import com.stripe.android.paymentsheet.utils.EventReporterProvider
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterialApi::class)
@Composable
internal fun FullScreenContent(
    modifier: Modifier,
    appBarState: LinkAppBarState,
    eventReporter: EventReporter,
    onBackPressed: () -> Unit,
    moveToWeb: () -> Unit,
    goBack: () -> Unit,
    handleViewAction: (LinkAction) -> Unit,
    onLinkScreenScreenCreated: () -> Unit,
    onNavControllerCreated: (NavHostController) -> Unit,
    navigate: (route: LinkScreen, clearStack: Boolean) -> Unit,
    dismissWithResult: ((LinkActivityResult) -> Unit)?,
    getLinkAccount: () -> LinkAccount?,
    changeEmail: () -> Unit,
) {
    val coroutineScope = rememberCoroutineScope()
    var bottomSheetContent by remember { mutableStateOf<BottomSheetContent?>(null) }

    val sheetState = rememberModalBottomSheetState(ModalBottomSheetValue.Hidden)
    val navController = rememberNavController()

    LaunchedEffect(bottomSheetContent) {
        if (bottomSheetContent != null) {
            sheetState.show()
        }
    }

    LaunchedEffect(sheetState.isVisible) {
        if (!sheetState.isVisible) {
            bottomSheetContent = null
        }
    }

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
                onUpdateSheetContent = { content ->
                    if (content != null) {
                        bottomSheetContent = content
                    } else {
                        coroutineScope.launch {
                            sheetState.hide()
                        }
                    }
                },
                onBackPressed = onBackPressed,
                moveToWeb = moveToWeb,
                handleViewAction = handleViewAction,
                navigate = navigate,
                dismissWithResult = dismissWithResult,
                getLinkAccount = getLinkAccount,
                goBack = goBack,
                changeEmail = changeEmail
            )
        }
    }

    LaunchedEffect(Unit) {
        onNavControllerCreated(navController)
        onLinkScreenScreenCreated()
    }
}
