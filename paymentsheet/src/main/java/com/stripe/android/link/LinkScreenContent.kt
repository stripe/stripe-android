package com.stripe.android.link

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavHostController
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
    onBackPressed: () -> Unit
) {
    val screenState by viewModel.linkScreenState.collectAsState()
    val appBarState by viewModel.linkAppBarState.collectAsState()

    LinkScreenContentBody(
        screenState = screenState,
        appBarState = appBarState,
        eventReporter = viewModel.eventReporter,
        onVerificationSucceeded = viewModel::onVerificationSucceeded,
        onDismissClicked = viewModel::onDismissVerificationClicked,
        onBackPressed = onBackPressed,
        // TODO remove too.
        navigate = viewModel::navigate,
        dismissWithResult = { result ->
            viewModel.dismissWithResult?.invoke(result)
        },
        getLinkAccount = {
            viewModel.linkAccount
        },
        handleViewAction = viewModel::handleViewAction,
        moveToWeb = viewModel::moveToWeb,
        goBack = viewModel::goBack,
        changeEmail = viewModel::changeEmail,
        onNavBackStackEntryChanged = viewModel::onNavEntryChanged,
        onLinkScreenScreenCreated = viewModel::linkScreenCreated,
        navigationChannel = viewModel.navigationFlow
    )
}

@Composable
internal fun LinkScreenContentBody(
    screenState: ScreenState,
    appBarState: LinkAppBarState,
    eventReporter: EventReporter,
    navigationChannel: SharedFlow<NavigationIntent>,
    onNavBackStackEntryChanged: (NavBackStackEntry?) -> Unit,
    onVerificationSucceeded: () -> Unit,
    onDismissClicked: () -> Unit,
    onBackPressed: () -> Unit,
    navigate: (route: LinkScreen, clearStack: Boolean) -> Unit,
    onLinkScreenScreenCreated: () -> Unit,
    dismissWithResult: ((LinkActivityResult) -> Unit)?,
    getLinkAccount: () -> LinkAccount?,
    handleViewAction: (LinkAction) -> Unit,
    moveToWeb: () -> Unit,
    goBack: () -> Unit,
    changeEmail: () -> Unit,
) {
    when (screenState) {
        ScreenState.FullScreen -> {
            FullScreenContent(
                modifier = Modifier
                    .testTag(FULL_SCREEN_CONTENT_TAG),
                onBackPressed = onBackPressed,
                appBarState = appBarState,
                navigate = navigate,
                dismissWithResult = dismissWithResult,
                getLinkAccount = getLinkAccount,
                moveToWeb = moveToWeb,
                goBack = goBack,
                eventReporter = eventReporter,
                onLinkScreenScreenCreated = onLinkScreenScreenCreated,
                handleViewAction = handleViewAction,
                changeEmail = changeEmail,
                onNavBackStackEntryChanged = onNavBackStackEntryChanged,
                navigationChannel = navigationChannel,
            )
        }
        ScreenState.Loading -> Unit
        is ScreenState.VerificationDialog -> {
            VerificationDialog(
                modifier = Modifier
                    .testTag(VERIFICATION_DIALOG_CONTENT_TAG),
                linkAccount = screenState.linkAccount,
                onVerificationSucceeded = onVerificationSucceeded,
                onDismissClicked = onDismissClicked
            )
        }
    }
}
