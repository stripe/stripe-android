package com.stripe.android.link

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.navigation.NavBackStackEntry
import com.stripe.android.link.model.LinkAccount
import com.stripe.android.link.ui.FullScreenContent
import com.stripe.android.link.ui.LinkAppBarState
import com.stripe.android.link.ui.verification.VerificationDialog
import com.stripe.android.paymentsheet.analytics.EventReporter
import com.stripe.android.uicore.navigation.NavigationIntent
import com.stripe.android.uicore.utils.collectAsState
import kotlinx.coroutines.flow.SharedFlow

@Composable
internal fun LinkScreenContent(
    viewModel: LinkActivityViewModel,
) {
    val screenState by viewModel.navigationManager.linkScreenState.collectAsState()
    val appBarState by viewModel.linkAppBarState.collectAsState()

    LinkScreenContentBody(
        screenState = screenState,
        appBarState = appBarState,
        eventReporter = viewModel.eventReporter,
        navigationEvents = viewModel.navigationManager.events,
        onBackPressed = viewModel::onBackPressed,
        dismissWithResult = { result ->
            viewModel.dismissWithResult?.invoke(result)
        },
        getLinkAccount = {
            viewModel.linkAccount
        },
        handleViewAction = viewModel::handleViewAction,
        moveToWeb = viewModel::moveToWeb,
        onNavBackStackEntryChanged = viewModel::handleBackStackChanged,
    )
}

@Composable
internal fun LinkScreenContentBody(
    screenState: ScreenState,
    appBarState: LinkAppBarState,
    eventReporter: EventReporter,
    navigationEvents: SharedFlow<NavigationIntent>,
    onBackPressed: () -> Unit,
    dismissWithResult: ((LinkActivityResult) -> Unit)?,
    getLinkAccount: () -> LinkAccount?,
    handleViewAction: (LinkAction) -> Unit,
    moveToWeb: () -> Unit,
    onNavBackStackEntryChanged: (NavBackStackEntry?) -> Unit,
) {
    when (screenState) {
        ScreenState.FullScreen -> {
            FullScreenContent(
                modifier = Modifier
                    .testTag(FULL_SCREEN_CONTENT_TAG),
                onBackPressed = onBackPressed,
                appBarState = appBarState,
                dismissWithResult = dismissWithResult,
                getLinkAccount = getLinkAccount,
                moveToWeb = moveToWeb,
                eventReporter = eventReporter,
                navigationEvents = navigationEvents,
                handleViewAction = handleViewAction,
                onNavBackStackEntryChanged = onNavBackStackEntryChanged,
            )
        }
        ScreenState.Loading -> Unit
        is ScreenState.VerificationDialog -> {
            VerificationDialog(
                modifier = Modifier
                    .testTag(VERIFICATION_DIALOG_CONTENT_TAG),
                linkAccount = screenState.linkAccount,
            )
        }
    }
}
