package com.stripe.android.link.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import com.stripe.android.common.ui.ElementsBottomSheetLayout
import com.stripe.android.link.LinkAction
import com.stripe.android.link.LinkActivityResult
import com.stripe.android.link.LinkScreen
import com.stripe.android.link.model.LinkAccount
import com.stripe.android.paymentsheet.analytics.EventReporter
import com.stripe.android.paymentsheet.utils.EventReporterProvider
import com.stripe.android.uicore.elements.bottomsheet.StripeBottomSheetState
import com.stripe.android.uicore.navigation.NavBackStackEntryUpdate
import com.stripe.android.uicore.navigation.NavigationEffects
import com.stripe.android.uicore.navigation.NavigationIntent
import com.stripe.android.uicore.navigation.rememberKeyboardController
import kotlinx.coroutines.flow.SharedFlow

@Composable
internal fun FullScreenContent(
    modifier: Modifier,
    bottomSheetState: StripeBottomSheetState,
    initialDestination: LinkScreen,
    appBarState: LinkAppBarState,
    eventReporter: EventReporter,
    onBackPressed: () -> Unit,
    moveToWeb: () -> Unit,
    goBack: () -> Unit,
    onNavBackStackEntryChanged: (NavBackStackEntryUpdate) -> Unit,
    navigationChannel: SharedFlow<NavigationIntent>,
    handleViewAction: (LinkAction) -> Unit,
    navigate: (route: LinkScreen, clearStack: Boolean) -> Unit,
    dismiss: () -> Unit,
    dismissWithResult: (LinkActivityResult) -> Unit,
    getLinkAccount: () -> LinkAccount?,
    changeEmail: () -> Unit
) {
    val navController = rememberNavController()
    val keyboardController = rememberKeyboardController()

    NavigationEffects(
        navigationChannel = navigationChannel,
        navHostController = navController,
        keyboardController = keyboardController,
        onBackStackEntryUpdated = onNavBackStackEntryChanged,
        onPopBackStackResult = { handled ->
            if (!handled) {
                dismiss()
            }
        },
    )

    ElementsBottomSheetLayout(
        state = bottomSheetState,
        onDismissed = dismiss,
    ) {
        EventReporterProvider(
            eventReporter = eventReporter
        ) {
            LinkContent(
                modifier = modifier,
                initialDestination = initialDestination,
                navController = navController,
                appBarState = appBarState,
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
}
