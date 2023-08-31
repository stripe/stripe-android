package com.stripe.android.financialconnections.ui

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.activity.addCallback
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.LocalUriHandler
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.airbnb.mvrx.MavericksView
import com.airbnb.mvrx.compose.collectAsState
import com.airbnb.mvrx.withState
import com.stripe.android.core.Logger
import com.stripe.android.financialconnections.browser.BrowserManager
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
import com.stripe.android.financialconnections.navigation.NavigationState
import com.stripe.android.financialconnections.navigation.toNavigationCommand
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

    @Inject
    lateinit var browserManager: BrowserManager

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
                        browserManager.createBrowserIntentForUrl(uri = Uri.parse(viewEffect.url))
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
        val lifecycleOwner = LocalLifecycleOwner.current
        var currentPane by remember { mutableStateOf(initialPane) }
        DisposableEffect(lifecycleOwner) {
            val lifecycle = lifecycleOwner.lifecycle
            val observer = ActivityVisibilityObserver(
                onBackgrounded = { viewModel.onBackgrounded(currentPane, true) },
                onForegrounded = { viewModel.onBackgrounded(currentPane, false) }
            )
            lifecycle.addObserver(observer)
            onDispose { lifecycle.removeObserver(observer) }
        }

        val navController = rememberNavController()
        val uriHandler = remember { CustomTabUriHandler(context, browserManager) }
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
                    currentPane = Pane.CONSENT
                    LaunchedPane(Pane.CONSENT)
                    BackHandler(navController, Pane.CONSENT)
                    ConsentScreen()
                }
                composable(NavigationDirections.manualEntry.destination) {
                    currentPane = Pane.MANUAL_ENTRY
                    LaunchedPane(Pane.MANUAL_ENTRY)
                    BackHandler(navController, Pane.MANUAL_ENTRY)
                    ManualEntryScreen()
                }
                composable(
                    route = NavigationDirections.ManualEntrySuccess.route,
                    arguments = NavigationDirections.ManualEntrySuccess.arguments
                ) {
                    currentPane = Pane.MANUAL_ENTRY_SUCCESS
                    LaunchedPane(Pane.MANUAL_ENTRY_SUCCESS)
                    BackHandler(navController, Pane.MANUAL_ENTRY_SUCCESS)
                    ManualEntrySuccessScreen(it)
                }
                composable(NavigationDirections.institutionPicker.destination) {
                    currentPane = Pane.INSTITUTION_PICKER
                    LaunchedPane(Pane.INSTITUTION_PICKER)
                    BackHandler(navController, Pane.INSTITUTION_PICKER)
                    InstitutionPickerScreen()
                }
                composable(NavigationDirections.partnerAuth.destination) {
                    currentPane = Pane.PARTNER_AUTH
                    LaunchedPane(Pane.PARTNER_AUTH)
                    BackHandler(navController, Pane.PARTNER_AUTH)
                    PartnerAuthScreen()
                }
                composable(NavigationDirections.accountPicker.destination) {
                    currentPane = Pane.ACCOUNT_PICKER
                    LaunchedPane(Pane.ACCOUNT_PICKER)
                    BackHandler(navController, Pane.ACCOUNT_PICKER)
                    AccountPickerScreen()
                }
                composable(NavigationDirections.success.destination) {
                    currentPane = Pane.SUCCESS
                    LaunchedPane(Pane.SUCCESS)
                    BackHandler(navController, Pane.SUCCESS)
                    SuccessScreen()
                }
                composable(NavigationDirections.reset.destination) {
                    currentPane = Pane.RESET
                    LaunchedPane(Pane.RESET)
                    BackHandler(navController, Pane.RESET)
                    ResetScreen()
                }
                composable(NavigationDirections.attachLinkedPaymentAccount.destination) {
                    currentPane = Pane.ATTACH_LINKED_PAYMENT_ACCOUNT
                    LaunchedPane(Pane.ATTACH_LINKED_PAYMENT_ACCOUNT)
                    BackHandler(navController, Pane.ATTACH_LINKED_PAYMENT_ACCOUNT)
                    AttachPaymentScreen()
                }
                composable(NavigationDirections.networkingLinkSignup.destination) {
                    currentPane = Pane.NETWORKING_LINK_SIGNUP_PANE
                    LaunchedPane(Pane.NETWORKING_LINK_SIGNUP_PANE)
                    BackHandler(navController, Pane.NETWORKING_LINK_SIGNUP_PANE)
                    NetworkingLinkSignupScreen()
                }
                composable(NavigationDirections.networkingLinkLoginWarmup.destination) {
                    currentPane = Pane.NETWORKING_LINK_LOGIN_WARMUP
                    LaunchedPane(Pane.NETWORKING_LINK_LOGIN_WARMUP)
                    BackHandler(navController, Pane.NETWORKING_LINK_LOGIN_WARMUP)
                    NetworkingLinkLoginWarmupScreen()
                }
                composable(NavigationDirections.networkingLinkVerification.destination) {
                    currentPane = Pane.NETWORKING_LINK_VERIFICATION
                    LaunchedPane(Pane.NETWORKING_LINK_VERIFICATION)
                    BackHandler(navController, Pane.NETWORKING_LINK_VERIFICATION)
                    NetworkingLinkVerificationScreen()
                }
                composable(NavigationDirections.networkingSaveToLinkVerification.destination) {
                    currentPane = Pane.NETWORKING_SAVE_TO_LINK_VERIFICATION
                    LaunchedPane(Pane.NETWORKING_SAVE_TO_LINK_VERIFICATION)
                    BackHandler(navController, Pane.NETWORKING_SAVE_TO_LINK_VERIFICATION)
                    NetworkingSaveToLinkVerificationScreen()
                }
                composable(NavigationDirections.linkAccountPicker.destination) {
                    currentPane = Pane.LINK_ACCOUNT_PICKER
                    LaunchedPane(Pane.LINK_ACCOUNT_PICKER)
                    BackHandler(navController, Pane.LINK_ACCOUNT_PICKER)
                    LinkAccountPickerScreen()
                }
                composable(NavigationDirections.linkStepUpVerification.destination) {
                    currentPane = Pane.LINK_STEP_UP_VERIFICATION
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
    private fun NavigationEffect(
        navController: NavHostController
    ) {
        val navigationState by navigationManager.navigationState.collectAsState()

        LaunchedEffect(navigationState) {
            logger.debug("updateNavigationState to $navigationState")
            val from = navController.currentDestination?.route
            when (val viewState = navigationState) {
                is NavigationState.NavigateToRoute -> {
                    navigateToRoute(viewState, from, navController)
                    navigationManager.onNavigated(navigationState)
                }

                is NavigationState.Idle -> {}
            }
        }
    }

    private fun navigateToRoute(
        viewState: NavigationState.NavigateToRoute,
        from: String?,
        navController: NavHostController
    ) {
        val destination = viewState.command.destination
        if (destination.isNotEmpty() && destination != from) {
            logger.debug("Navigating from $from to $destination")
            navController.navigate(destination) {
                launchSingleTop = true
                val currentScreen: String? = navController.currentBackStackEntry?.destination?.route
                if (currentScreen != null && viewState.popCurrentFromBackStack) {
                    popUpTo(currentScreen) { inclusive = true }
                }
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

/**
 * Observer that will notify the view model when the activity is moved to the background or
 * brought back to the foreground.
 */
private class ActivityVisibilityObserver(
    val onBackgrounded: () -> Unit,
    val onForegrounded: () -> Unit
) : DefaultLifecycleObserver {

    private var isFirstStart = true
    private var isInBackground = false

    override fun onStart(owner: LifecycleOwner) {
        super.onStart(owner)
        if (!isFirstStart && isInBackground) {
            onForegrounded()
        }
        isFirstStart = false
        isInBackground = false
    }

    override fun onStop(owner: LifecycleOwner) {
        super.onStop(owner)
        // If the activity is being rotated, we don't want to notify a backgrounded state
        val changingConfigurations = (owner as? AppCompatActivity)?.isChangingConfigurations ?: false
        if (!changingConfigurations) {
            isInBackground = true
            onBackgrounded()
        }
    }
}
