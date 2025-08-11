package com.stripe.android.link

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import com.stripe.android.link.model.LinkAccount
import com.stripe.android.link.model.LinkAppearance
import com.stripe.android.link.ui.FullScreenContent
import com.stripe.android.link.ui.LinkAppBarState
import com.stripe.android.link.ui.LinkContentScrollHandler
import com.stripe.android.link.ui.LocalLinkContentScrollHandler
import com.stripe.android.link.ui.verification.VerificationDialog
import com.stripe.android.paymentsheet.analytics.EventReporter
import com.stripe.android.uicore.elements.bottomsheet.StripeBottomSheetState
import com.stripe.android.uicore.navigation.NavBackStackEntryUpdate
import com.stripe.android.uicore.navigation.NavigationIntent
import com.stripe.android.uicore.utils.collectAsState
import kotlinx.coroutines.flow.SharedFlow

@Composable
internal fun LinkScreenContent(
    viewModel: LinkActivityViewModel,
    bottomSheetState: StripeBottomSheetState,
) {
    val screenState by viewModel.linkScreenState.collectAsState()
    val appBarState by viewModel.linkAppBarState.collectAsState()

    val linkContentScrollHandler = remember(viewModel) {
        LinkContentScrollHandler(onCanScrollBackwardChanged = viewModel::onContentCanScrollBackwardChanged)
    }

    CompositionLocalProvider(
        LocalLinkContentScrollHandler provides linkContentScrollHandler,
    ) {
        LinkScreenContentBody(
            bottomSheetState = bottomSheetState,
            screenState = screenState,
            appBarState = appBarState,
            eventReporter = viewModel.eventReporter,
            onVerificationSucceeded = viewModel::onVerificationSucceeded,
            onDismissClicked = viewModel::onDismissVerificationClicked,
            onBackPressed = viewModel::goBack,
            navigate = viewModel::navigate,
            dismiss = viewModel::dismissSheet,
            dismissWithResult = viewModel::handleResult,
            getLinkAccount = {
                viewModel.linkAccount
            },
            handleViewAction = viewModel::handleViewAction,
            moveToWeb = viewModel::moveToWeb,
            goBack = viewModel::goBack,
            changeEmail = viewModel::changeEmail,
            onNavBackStackEntryChanged = viewModel::onNavEntryChanged,
            navigationChannel = viewModel.navigationFlow,
            appearance = viewModel.linkConfiguration.linkAppearance
        )
    }
}

@Composable
internal fun LinkScreenContentBody(
    bottomSheetState: StripeBottomSheetState,
    screenState: ScreenState,
    appBarState: LinkAppBarState,
    eventReporter: EventReporter,
    appearance: LinkAppearance?,
    navigationChannel: SharedFlow<NavigationIntent>,
    onNavBackStackEntryChanged: (NavBackStackEntryUpdate) -> Unit,
    onVerificationSucceeded: () -> Unit,
    onDismissClicked: () -> Unit,
    onBackPressed: () -> Unit,
    navigate: (route: LinkScreen, clearStack: Boolean) -> Unit,
    dismiss: () -> Unit,
    dismissWithResult: (LinkActivityResult) -> Unit,
    getLinkAccount: () -> LinkAccount?,
    handleViewAction: (LinkAction) -> Unit,
    moveToWeb: (Throwable) -> Unit,
    goBack: () -> Unit,
    changeEmail: () -> Unit,
) {
    when (screenState) {
        is ScreenState.FullScreen -> {
            FullScreenContent(
                modifier = Modifier
                    .testTag(FULL_SCREEN_CONTENT_TAG),
                bottomSheetState = bottomSheetState,
                initialDestination = screenState.initialDestination,
                onBackPressed = onBackPressed,
                appBarState = appBarState,
                navigate = navigate,
                dismiss = dismiss,
                dismissWithResult = dismissWithResult,
                getLinkAccount = getLinkAccount,
                moveToWeb = moveToWeb,
                goBack = goBack,
                eventReporter = eventReporter,
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
                changeEmail = changeEmail,
                onDismissClicked = onDismissClicked,
                dismissWithResult = dismissWithResult,
                linkAppearance = appearance
            )
        }
    }
}
