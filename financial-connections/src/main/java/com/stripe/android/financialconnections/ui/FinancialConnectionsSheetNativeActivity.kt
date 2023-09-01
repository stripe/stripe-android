package com.stripe.android.financialconnections.ui

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.addCallback
import androidx.activity.compose.BackHandler
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
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController
import com.airbnb.mvrx.MavericksView
import com.airbnb.mvrx.compose.collectAsState
import com.airbnb.mvrx.withState
import com.stripe.android.core.Logger
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
import com.stripe.android.financialconnections.navigation.Destination
import com.stripe.android.financialconnections.navigation.NavigationIntent
import com.stripe.android.financialconnections.navigation.composable
import com.stripe.android.financialconnections.navigation.destination
import com.stripe.android.financialconnections.navigation.pane
import com.stripe.android.financialconnections.presentation.CreateBrowserIntentForUrl
import com.stripe.android.financialconnections.presentation.FinancialConnectionsSheetNativeViewEffect.Finish
import com.stripe.android.financialconnections.presentation.FinancialConnectionsSheetNativeViewEffect.OpenUrl
import com.stripe.android.financialconnections.presentation.FinancialConnectionsSheetNativeViewModel
import com.stripe.android.financialconnections.ui.theme.FinancialConnectionsTheme
import com.stripe.android.financialconnections.utils.argsOrNull
import com.stripe.android.financialconnections.utils.viewModelLazy
import com.stripe.android.uicore.image.StripeImageLoader
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import javax.inject.Inject

internal class FinancialConnectionsSheetNativeActivity : AppCompatActivity(), MavericksView {

    val args by argsOrNull<FinancialConnectionsSheetNativeActivityArgs>()

    val viewModel: FinancialConnectionsSheetNativeViewModel by viewModelLazy()

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
        val initialDestination = remember(initialPane) { initialPane.destination }
        NavigationEffects(viewModel.navigationChannel, navController)

        CompositionLocalProvider(
            LocalReducedBranding provides reducedBranding,
            LocalNavHostController provides navController,
            LocalImageLoader provides imageLoader,
            LocalUriHandler provides uriHandler
        ) {
            LaunchedEffect(Unit) {
                navController.addOnDestinationChangedListener { _, navDestination, _ ->
                    viewModel.onPaneLaunched(navDestination.pane)
                }
            }
            BackHandler(true) {
                viewModel.onBackClick(navController.currentDestination?.pane)
                if (navController.popBackStack().not()) onBackPressedDispatcher.onBackPressed()
            }
            NavHost(
                navController,
                startDestination = initialDestination.fullRoute,
            ) {
                composable(Destination.Consent) {
                    ConsentScreen()
                }
                composable(Destination.ManualEntry) {
                    ManualEntryScreen()
                }

                composable(Destination.PartnerAuth) {
                    PartnerAuthScreen()
                }
                composable(Destination.InstitutionPicker) {
                    InstitutionPickerScreen()
                }
                composable(Destination.AccountPicker) {
                    AccountPickerScreen()
                }
                composable(Destination.Success) {
                    SuccessScreen()
                }
                composable(Destination.Reset) {
                    ResetScreen()
                }
                composable(Destination.AttachLinkedPaymentAccount) {
                    AttachPaymentScreen()
                }
                composable(Destination.NetworkingLinkSignup) {
                    NetworkingLinkSignupScreen()
                }
                composable(Destination.NetworkingLinkLoginWarmup) {
                    NetworkingLinkLoginWarmupScreen()
                }
                composable(Destination.NetworkingLinkVerification) {
                    NetworkingLinkVerificationScreen()
                }
                composable(Destination.NetworkingSaveToLinkVerification) {
                    NetworkingSaveToLinkVerificationScreen()
                }
                composable(Destination.LinkAccountPicker) {
                    LinkAccountPickerScreen()
                }
                composable(Destination.LinkStepUpVerification) {
                    LinkStepUpVerificationScreen()
                }
                composable(Destination.ManualEntrySuccess) {
                    ManualEntrySuccessScreen(it)
                }
            }
        }
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
    fun NavigationEffects(
        navigationChannel: Channel<NavigationIntent>,
        navHostController: NavHostController
    ) {
        val activity = (LocalContext.current as? Activity)
        LaunchedEffect(activity, navHostController, navigationChannel) {
            navigationChannel.receiveAsFlow().collect { intent ->
                if (activity?.isFinishing == true) {
                    return@collect
                }
                when (intent) {
                    is NavigationIntent.NavigateBack -> {
                        if (intent.route != null) {
                            navHostController.popBackStack(intent.route, intent.inclusive)
                        } else {
                            navHostController.popBackStack()
                        }
                    }

                    is NavigationIntent.NavigateTo -> {
                        val from = navHostController.currentDestination?.route
                        val destination = intent.route
                        if (destination.isNotEmpty() && destination != from) {
                            logger.debug("Navigating from $from to $destination")
                            navHostController.navigate(destination) {
                                launchSingleTop = intent.isSingleTop
                                if (from != null && intent.popUpToCurrent) {
                                    popUpTo(from) { inclusive = true }
                                }
                            }
                        }
                    }
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
