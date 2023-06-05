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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.navigation.NavHostController
import androidx.navigation.NavOptionsBuilder
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.airbnb.mvrx.MavericksView
import com.airbnb.mvrx.compose.collectAsState
import com.airbnb.mvrx.withState
import com.stripe.android.core.Logger
import com.stripe.android.financialconnections.domain.toNavigationCommand
import com.stripe.android.financialconnections.features.accountpicker.AccountPickerScreen
import com.stripe.android.financialconnections.features.attachpayment.AttachPaymentScreen
import com.stripe.android.financialconnections.features.common.CloseDialog
import com.stripe.android.financialconnections.features.consent.ConsentScreen
import com.stripe.android.financialconnections.features.institutionpicker.InstitutionPickerScreen
import com.stripe.android.financialconnections.features.linkaccountpicker.LinkAccountPickerScreen
import com.stripe.android.financialconnections.features.linkstepupverification.LinkStepUpVerificationScreen
import com.stripe.android.financialconnections.features.manualentry.ManualEntryScreen
import com.stripe.android.financialconnections.features.manualentrysuccess.ManualEntrySuccessScreen
import com.stripe.android.financialconnections.features.networkinglinkloginwarmup.NetworkingLinkLoginWarmupScreen
import com.stripe.android.financialconnections.features.networkinglinksignup.NetworkingLinkSignupScreen
import com.stripe.android.financialconnections.features.networkinglinkverification.NetworkingLinkVerificationScreen
import com.stripe.android.financialconnections.features.networkingsavetolinkverification.NetworkingSaveToLinkVerificationScreen
import com.stripe.android.financialconnections.features.partnerauth.PartnerAuthScreen
import com.stripe.android.financialconnections.features.reset.ResetScreen
import com.stripe.android.financialconnections.features.success.SuccessScreen
import com.stripe.android.financialconnections.launcher.FinancialConnectionsSheetNativeActivityArgs
import com.stripe.android.financialconnections.model.FinancialConnectionsSessionManifest.Pane
import com.stripe.android.financialconnections.navigation.NavigationDirections
import com.stripe.android.financialconnections.navigation.NavigationManager
import com.stripe.android.financialconnections.presentation.CreateBrowserIntentForUrl
import com.stripe.android.financialconnections.presentation.FinancialConnectionsSheetNativeViewEffect.Finish
import com.stripe.android.financialconnections.presentation.FinancialConnectionsSheetNativeViewEffect.OpenUrl
import com.stripe.android.financialconnections.presentation.FinancialConnectionsSheetNativeViewModel
import com.stripe.android.financialconnections.ui.theme.FinancialConnectionsTheme
import com.stripe.android.financialconnections.utils.argsOrNull
import com.stripe.android.financialconnections.utils.viewModelLazy
import com.stripe.android.uicore.image.StripeImageLoader
import javax.inject.Inject

internal class FinancialConnectionsSheetNativeActivity : AppCompatActivity(), MavericksView {

    val args by argsOrNull<FinancialConnectionsSheetNativeActivityArgs>()

    val viewModel: FinancialConnectionsSheetNativeViewModel by viewModelLazy()

    @Inject
    lateinit var navigationManager: NavigationManager

    @Inject
    lateinit var logger: Logger

    @Inject
    lateinit var imageLoader: StripeImageLoader

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (args == null) {
            finish()
        } else {
            viewModel.activityRetainedComponent.inject(this)
            viewModel.onEach { postInvalidate() }
            onBackPressedDispatcher.addCallback { viewModel.onBackPressed() }
            setContent {
                FinancialConnectionsTheme {
                    Column {
                        Box(modifier = Modifier.weight(1f)) {
                            val closeDialog = viewModel.collectAsState { it.closeDialog }
                            val firstPane =
                                viewModel.collectAsState { it.initialPane }
                            val reducedBranding =
                                viewModel.collectAsState { it.reducedBranding }
                            closeDialog.value?.let {
                                CloseDialog(
                                    description = it.description,
                                    onConfirmClick = viewModel::onCloseConfirm,
                                    onDismissClick = viewModel::onCloseDismiss
                                )
                            }
                            NavHost(
                                firstPane.value,
                                reducedBranding.value
                            )
                        }
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
    @Suppress("LongMethod")
    @Composable
    fun NavHost(
        initialPane: Pane,
        reducedBranding: Boolean
    ) {
        val context = LocalContext.current
        val navController = rememberNavController()
        val uriHandler = remember { CustomTabUriHandler(context) }
        val initialDestination =
            remember(initialPane) {
                initialPane.toNavigationCommand(
                    emptyMap()
                ).destination
            }
        NavigationEffect(navController)
        CompositionLocalProvider(
            LocalReducedBranding provides reducedBranding,
            LocalNavHostController provides navController,
            LocalImageLoader provides imageLoader,
            LocalUriHandler provides uriHandler
        ) {
            NavHost(navController, startDestination = initialDestination) {
                composable(NavigationDirections.consent.destination) {
                    LaunchedPane(Pane.CONSENT)
                    BackHandler(navController, Pane.CONSENT)
                    ConsentScreen()
                }
                composable(NavigationDirections.manualEntry.destination) {
                    LaunchedPane(Pane.MANUAL_ENTRY)
                    BackHandler(navController, Pane.MANUAL_ENTRY)
                    ManualEntryScreen()
                }
                composable(
                    route = NavigationDirections.ManualEntrySuccess.route,
                    arguments = NavigationDirections.ManualEntrySuccess.arguments
                ) {
                    LaunchedPane(Pane.MANUAL_ENTRY_SUCCESS)
                    BackHandler(navController, Pane.MANUAL_ENTRY_SUCCESS)
                    ManualEntrySuccessScreen(it)
                }
                composable(NavigationDirections.institutionPicker.destination) {
                    LaunchedPane(Pane.INSTITUTION_PICKER)
                    BackHandler(navController, Pane.INSTITUTION_PICKER)
                    InstitutionPickerScreen()
                }
                composable(NavigationDirections.partnerAuth.destination) {
                    LaunchedPane(Pane.PARTNER_AUTH)
                    BackHandler(navController, Pane.PARTNER_AUTH)
                    PartnerAuthScreen()
                }
                composable(NavigationDirections.accountPicker.destination) {
                    LaunchedPane(Pane.ACCOUNT_PICKER)
                    BackHandler(navController, Pane.ACCOUNT_PICKER)
                    AccountPickerScreen()
                }
                composable(NavigationDirections.success.destination) {
                    LaunchedPane(Pane.SUCCESS)
                    BackHandler(navController, Pane.SUCCESS)
                    SuccessScreen()
                }
                composable(NavigationDirections.reset.destination) {
                    LaunchedPane(Pane.RESET)
                    BackHandler(navController, Pane.RESET)
                    ResetScreen()
                }
                composable(NavigationDirections.attachLinkedPaymentAccount.destination) {
                    LaunchedPane(Pane.ATTACH_LINKED_PAYMENT_ACCOUNT)
                    BackHandler(navController, Pane.ATTACH_LINKED_PAYMENT_ACCOUNT)
                    AttachPaymentScreen()
                }
                composable(NavigationDirections.networkingLinkSignup.destination) {
                    LaunchedPane(Pane.NETWORKING_LINK_SIGNUP_PANE)
                    BackHandler(navController, Pane.NETWORKING_LINK_SIGNUP_PANE)
                    NetworkingLinkSignupScreen()
                }
                composable(NavigationDirections.networkingLinkLoginWarmup.destination) {
                    LaunchedPane(Pane.NETWORKING_LINK_LOGIN_WARMUP)
                    BackHandler(navController, Pane.NETWORKING_LINK_LOGIN_WARMUP)
                    NetworkingLinkLoginWarmupScreen()
                }
                composable(NavigationDirections.networkingLinkVerification.destination) {
                    LaunchedPane(Pane.NETWORKING_LINK_VERIFICATION)
                    BackHandler(navController, Pane.NETWORKING_LINK_VERIFICATION)
                    NetworkingLinkVerificationScreen()
                }
                composable(NavigationDirections.networkingSaveToLinkVerification.destination) {
                    LaunchedPane(Pane.NETWORKING_SAVE_TO_LINK_VERIFICATION)
                    BackHandler(navController, Pane.NETWORKING_SAVE_TO_LINK_VERIFICATION)
                    NetworkingSaveToLinkVerificationScreen()
                }
                composable(NavigationDirections.linkAccountPicker.destination) {
                    LaunchedPane(Pane.LINK_ACCOUNT_PICKER)
                    BackHandler(navController, Pane.LINK_ACCOUNT_PICKER)
                    LinkAccountPickerScreen()
                }
                composable(NavigationDirections.linkStepUpVerification.destination) {
                    LaunchedPane(Pane.LINK_STEP_UP_VERIFICATION)
                    BackHandler(navController, Pane.LINK_STEP_UP_VERIFICATION)
                    LinkStepUpVerificationScreen()
                }
            }
        }
    }

    @Composable
    private fun BackHandler(navController: NavHostController, pane: Pane) {
        androidx.activity.compose.BackHandler(true) {
            viewModel.onBackClick(pane)
            if (navController.popBackStack().not()) onBackPressedDispatcher.onBackPressed()
        }
    }

    @Composable
    private fun LaunchedPane(
        pane: Pane
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
                        launchSingleTop = true
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

internal val LocalReducedBranding = staticCompositionLocalOf<Boolean> {
    error("No ReducedBranding provided")
}

internal val LocalImageLoader = staticCompositionLocalOf<StripeImageLoader> {
    error("No ImageLoader provided")
}
