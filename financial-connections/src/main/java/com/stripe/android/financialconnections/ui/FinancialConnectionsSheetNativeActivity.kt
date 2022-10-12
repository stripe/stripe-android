package com.stripe.android.financialconnections.ui

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.addCallback
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavOptionsBuilder
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.airbnb.mvrx.MavericksView
import com.airbnb.mvrx.compose.collectAsState
import com.airbnb.mvrx.viewModel
import com.airbnb.mvrx.withState
import com.stripe.android.core.Logger
import com.stripe.android.financialconnections.domain.toNavigationCommand
import com.stripe.android.financialconnections.features.accountpicker.AccountPickerScreen
import com.stripe.android.financialconnections.features.attachpayment.AttachPaymentScreen
import com.stripe.android.financialconnections.features.common.CloseDialog
import com.stripe.android.financialconnections.features.consent.ConsentScreen
import com.stripe.android.financialconnections.features.institutionpicker.InstitutionPickerScreen
import com.stripe.android.financialconnections.features.manualentry.ManualEntryScreen
import com.stripe.android.financialconnections.features.manualentrysuccess.ManualEntrySuccessScreen
import com.stripe.android.financialconnections.features.partnerauth.PartnerAuthScreen
import com.stripe.android.financialconnections.features.reset.ResetScreen
import com.stripe.android.financialconnections.features.success.SuccessScreen
import com.stripe.android.financialconnections.model.FinancialConnectionsSessionManifest.NextPane
import com.stripe.android.financialconnections.navigation.NavigationDirections
import com.stripe.android.financialconnections.navigation.NavigationManager
import com.stripe.android.financialconnections.presentation.CreateBrowserIntentForUrl
import com.stripe.android.financialconnections.presentation.FinancialConnectionsSheetNativeViewEffect.Finish
import com.stripe.android.financialconnections.presentation.FinancialConnectionsSheetNativeViewEffect.OpenUrl
import com.stripe.android.financialconnections.presentation.FinancialConnectionsSheetNativeViewModel
import com.stripe.android.financialconnections.ui.theme.FinancialConnectionsTheme
import com.stripe.android.uicore.image.StripeImageLoader
import javax.inject.Inject

internal class FinancialConnectionsSheetNativeActivity : AppCompatActivity(), MavericksView {

    val viewModel: FinancialConnectionsSheetNativeViewModel by viewModel()

    @Inject
    lateinit var navigationManager: NavigationManager

    @Inject
    lateinit var logger: Logger

    @Inject
    lateinit var imageLoader: StripeImageLoader

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel.activityRetainedComponent.inject(this)
        viewModel.onEach { postInvalidate() }
        onBackPressedDispatcher.addCallback { viewModel.onBackPressed() }
        setContent {
            FinancialConnectionsTheme {
                Column {
                    Box(modifier = Modifier.weight(1f)) {
                        val showCloseDialog = viewModel.collectAsState { it.showCloseDialog }
                        val firstPane = viewModel.collectAsState(mapper = { it.initialPane })
                        if (showCloseDialog.value) CloseDialog(
                            viewModel::onCloseConfirm,
                            viewModel::onCloseDismiss
                        )
                        NavHost(firstPane.value)
                    }
                }
            }
        }
    }

    /**
     * handle state changes here.
     */
    override fun invalidate() {
        withState(viewModel) { state ->
            state.viewEffect?.let { viewEffect ->
                when (viewEffect) {
                    is OpenUrl -> startActivity(
                        CreateBrowserIntentForUrl(
                            context = this,
                            uri = Uri.parse(viewEffect.url)
                        )
                    )
                    is Finish -> {
                        setResult(
                            Activity.RESULT_OK,
                            Intent().putExtra(EXTRA_RESULT, viewEffect.result)
                        )
                        finish()
                    }
                }
                viewModel.onViewEffectLaunched()
            }
        }
    }

    @OptIn(ExperimentalMaterialApi::class)
    @Composable
    fun NavHost(initialPane: NextPane) {
        val navController = rememberNavController()
        val initialDestination =
            remember(initialPane) { initialPane.toNavigationCommand(logger, emptyMap()).destination }
        NavigationEffect(navController)
        CompositionLocalProvider(
            LocalNavHostController provides navController,
            LocalImageLoader provides imageLoader,
        ) {
            NavHost(navController, startDestination = initialDestination) {
                composable(NavigationDirections.consent.destination) {
                    LaunchedPane(NextPane.CONSENT)
                    ConsentScreen()
                }
                composable(NavigationDirections.manualEntry.destination) {
                    LaunchedPane(NextPane.MANUAL_ENTRY)
                    ManualEntryScreen()
                }
                composable(
                    route = NavigationDirections.ManualEntrySuccess.route,
                    arguments = NavigationDirections.ManualEntrySuccess.arguments
                ) {
                    LaunchedPane(NextPane.MANUAL_ENTRY_SUCCESS)
                    ManualEntrySuccessScreen(it)
                }
                composable(NavigationDirections.institutionPicker.destination) {
                    LaunchedPane(NextPane.INSTITUTION_PICKER)
                    InstitutionPickerScreen()
                }
                composable(NavigationDirections.partnerAuth.destination) {
                    LaunchedPane(NextPane.PARTNER_AUTH)
                    PartnerAuthScreen()
                }
                composable(NavigationDirections.accountPicker.destination) {
                    LaunchedPane(NextPane.ACCOUNT_PICKER)
                    AccountPickerScreen()
                }
                composable(NavigationDirections.success.destination) {
                    LaunchedPane(NextPane.SUCCESS)
                    SuccessScreen()
                }
                composable(NavigationDirections.reset.destination) {
                    LaunchedPane(NextPane.RESET)
                    ResetScreen()
                }
                composable(NavigationDirections.attachLinkedPaymentAccount.destination) {
                    LaunchedPane(NextPane.ATTACH_LINKED_PAYMENT_ACCOUNT)
                    AttachPaymentScreen()
                }
            }
        }
    }

    @Composable
    private fun LaunchedPane(
        pane: NextPane
    ) {
        LaunchedEffect(Unit) { viewModel.onPaneLaunched(pane) }
    }

    /**
     * Handles new intents in the form of the redirect from the custom tab hosted auth flow
     */
    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        viewModel.handleOnNewIntent(intent)
    }

    override fun onResume() {
        super.onResume()
        viewModel.onResume()
    }

    @Composable
    private fun NavigationEffect(navController: NavHostController) {
        LaunchedEffect(navigationManager.commands) {
            navigationManager.commands.collect { command ->
                if (command.destination.isNotEmpty()) {
                    navController.navigate(command.destination) {
                        popUpIfNotBackwardsNavigable(navController)
                    }
                }
            }
        }
    }

    /**
     * Removes screens that are not backwards-navigable from the backstack.
     */
    private fun NavOptionsBuilder.popUpIfNotBackwardsNavigable(navController: NavHostController) {
        val destination: String = navController.currentBackStackEntry?.destination?.route ?: return
        val destinationsToSkipOnBack = listOf(
            NavigationDirections.partnerAuth.destination,
            NavigationDirections.reset.destination
        )
        if (destinationsToSkipOnBack.contains(navController.currentDestination?.route)
        ) {
            popUpTo(destination) {
                inclusive = true
            }
        }
    }

    internal companion object {
        internal const val EXTRA_RESULT = "result"
    }
}

internal val LocalNavHostController = staticCompositionLocalOf<NavHostController> {
    error("No NavHostController provided")
}

internal val LocalImageLoader = staticCompositionLocalOf<StripeImageLoader> {
    error("No ImageLoader provided")
}
