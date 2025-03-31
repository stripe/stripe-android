package com.stripe.android.financialconnections.ui

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.addCallback
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.material.ModalBottomSheetValue
import androidx.compose.material.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController
import com.stripe.android.core.Logger
import com.stripe.android.financialconnections.browser.BrowserManager
import com.stripe.android.financialconnections.launcher.FinancialConnectionsSheetNativeActivityArgs
import com.stripe.android.financialconnections.model.FinancialConnectionsSessionManifest
import com.stripe.android.financialconnections.model.FinancialConnectionsSessionManifest.Pane
import com.stripe.android.financialconnections.navigation.Destination
import com.stripe.android.financialconnections.navigation.bottomSheet
import com.stripe.android.financialconnections.navigation.bottomsheet.BottomSheetNavigator
import com.stripe.android.financialconnections.navigation.composable
import com.stripe.android.financialconnections.navigation.destination
import com.stripe.android.financialconnections.navigation.pane
import com.stripe.android.financialconnections.navigation.topappbar.TopAppBarHost
import com.stripe.android.financialconnections.presentation.FinancialConnectionsSheetNativeViewEffect.Finish
import com.stripe.android.financialconnections.presentation.FinancialConnectionsSheetNativeViewEffect.OpenUrl
import com.stripe.android.financialconnections.presentation.FinancialConnectionsSheetNativeViewModel
import com.stripe.android.financialconnections.ui.components.FinancialConnectionsBottomSheetLayout
import com.stripe.android.financialconnections.ui.components.FinancialConnectionsModalBottomSheetLayout
import com.stripe.android.financialconnections.ui.components.FinancialConnectionsScaffold
import com.stripe.android.financialconnections.ui.components.FinancialConnectionsTopAppBar
import com.stripe.android.financialconnections.ui.theme.FinancialConnectionsTheme
import com.stripe.android.financialconnections.ui.theme.Theme
import com.stripe.android.uicore.elements.bottomsheet.rememberStripeBottomSheetState
import com.stripe.android.uicore.image.StripeImageLoader
import com.stripe.android.uicore.navigation.NavigationEffects
import com.stripe.android.uicore.navigation.rememberKeyboardController
import com.stripe.android.uicore.utils.collectAsState
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject

internal class FinancialConnectionsSheetNativeActivity : AppCompatActivity() {

    val viewModel: FinancialConnectionsSheetNativeViewModel by viewModels(
        factoryProducer = { FinancialConnectionsSheetNativeViewModel.Factory }
    )

    private var visibilityObserver: ActivityVisibilityObserver? = null

    @Inject
    lateinit var logger: Logger

    @Inject
    lateinit var imageLoader: StripeImageLoader

    @Inject
    lateinit var browserManager: BrowserManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val args = getArgs(intent)
        if (args == null) {
            finish()
            return
        }

        viewModel.activityRetainedComponent.inject(this)

        observeBackPress()
        observeBackgroundEvents()
        observeViewEffects()

        setContent {
            FinancialConnectionsTheme(args.theme) {
                val state by viewModel.stateFlow.collectAsState()
                val bottomSheetState = rememberStripeBottomSheetState(
                    initialValue = ModalBottomSheetValue.Expanded,
                )

                FinancialConnectionsBottomSheetLayout(
                    state = bottomSheetState,
                    onDismissed = viewModel::onBackPressed,
                ) {
                    NavHost(
                        initialPane = state.initialPane,
                        testMode = state.testMode,
                    )
                }
            }
        }
    }

    private fun observeBackPress() {
        onBackPressedDispatcher.addCallback { viewModel.onBackPressed() }
    }

    private fun observeBackgroundEvents() {
        visibilityObserver = ActivityVisibilityObserver(
            onBackgrounded = viewModel::onBackgrounded,
            onForegrounded = viewModel::onForegrounded,
        ).also {
            lifecycle.addObserver(it)
        }
    }

    private fun observeViewEffects() = lifecycleScope.launch {
        repeatOnLifecycle(Lifecycle.State.STARTED) {
            viewModel.stateFlow
                .map { it.viewEffect }
                .distinctUntilChanged()
                .filterNotNull()
                .collect { viewEffect ->
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

    @Composable
    fun NavHost(
        initialPane: Pane,
        testMode: Boolean,
    ) {
        val context = LocalContext.current
        val uriHandler = remember { CustomTabUriHandler(context, browserManager) }
        val initialDestination = remember(initialPane) { initialPane.destination }
        val topAppBarState by viewModel.topAppBarState.collectAsState()

        val sheetState = rememberModalBottomSheetState(
            ModalBottomSheetValue.Hidden,
            skipHalfExpanded = true
        )

        val bottomSheetNavigator = remember { BottomSheetNavigator(sheetState) }
        val navController = rememberNavController(bottomSheetNavigator)
        val keyboardController = rememberKeyboardController()

        NavigationEffects(viewModel.navigationFlow, navController, keyboardController) { backStackEntry ->
            val pane = backStackEntry?.destination?.pane
            if (pane != null) {
                viewModel.handlePaneChanged(pane)
            }
        }

        CompositionLocalProvider(
            LocalTestMode provides testMode,
            LocalNavHostController provides navController,
            LocalImageLoader provides imageLoader,
            LocalUriHandler provides uriHandler,
            LocalTopAppBarHost provides viewModel,
        ) {
            BackHandler(true) {
                viewModel.onBackClick(navController.currentDestination?.pane)
                if (navController.popBackStack().not()) viewModel.onBackPressed()
            }
            FinancialConnectionsModalBottomSheetLayout(
                bottomSheetNavigator = bottomSheetNavigator,
            ) {
                FinancialConnectionsScaffold(
                    topBar = {
                        FinancialConnectionsTopAppBar(
                            state = topAppBarState,
                            onCloseClick = viewModel::handleOnCloseClick,
                        )
                    },
                ) {
                    NavHost(
                        navController = navController,
                        startDestination = initialDestination.fullRoute,
                    ) {
                        composable(Destination.Consent)
                        composable(Destination.ManualEntry)
                        composable(Destination.PartnerAuth)
                        bottomSheet(Destination.PartnerAuthDrawer)
                        bottomSheet(Destination.Exit)
                        composable(Destination.InstitutionPicker)
                        composable(Destination.AccountPicker)
                        composable(Destination.Success)
                        composable(Destination.Reset)
                        composable(Destination.Error)
                        composable(Destination.AttachLinkedPaymentAccount)
                        composable(Destination.NetworkingLinkSignup)
                        bottomSheet(Destination.NetworkingLinkLoginWarmup)
                        composable(Destination.NetworkingLinkVerification)
                        composable(Destination.NetworkingSaveToLinkVerification)
                        composable(Destination.LinkAccountPicker)
                        composable(Destination.BankAuthRepair)
                        composable(Destination.LinkStepUpVerification)
                        composable(Destination.ManualEntrySuccess)
                        bottomSheet(Destination.Notice)
                        bottomSheet(Destination.AccountUpdateRequired)
                        composable(Destination.LinkLogin)
                    }
                }
            }
        }
    }

    /**
     * Handles new intents in the form of the redirect from the custom tab hosted auth flow
     */
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        viewModel.handleOnNewIntent(intent)
    }

    override fun onResume() {
        super.onResume()
        viewModel.onResume()
    }

    override fun onDestroy() {
        visibilityObserver?.let {
            lifecycle.removeObserver(it)
        }
        super.onDestroy()
    }

    internal companion object {
        internal const val EXTRA_RESULT = "result"

        private const val EXTRA_ARGS = "FinancialConnectionsSheetNativeActivityArgs"
        fun intent(context: Context, args: FinancialConnectionsSheetNativeActivityArgs): Intent {
            return Intent(context, FinancialConnectionsSheetNativeActivity::class.java).apply {
                putExtra(EXTRA_ARGS, args)
            }
        }

        fun getArgs(savedStateHandle: SavedStateHandle) =
            savedStateHandle.get<FinancialConnectionsSheetNativeActivityArgs>(EXTRA_ARGS)

        fun getArgs(intent: Intent): FinancialConnectionsSheetNativeActivityArgs? {
            return intent.getParcelableExtra(EXTRA_ARGS)
        }
    }
}

internal val LocalNavHostController = staticCompositionLocalOf<NavHostController> {
    error("No NavHostController provided")
}

internal val LocalTestMode = staticCompositionLocalOf<Boolean> {
    error("No TestMode provided")
}

internal val LocalImageLoader = staticCompositionLocalOf<StripeImageLoader> {
    error("No ImageLoader provided")
}

internal val LocalTopAppBarHost = staticCompositionLocalOf<TopAppBarHost> {
    error("No TopAppBarHost provided")
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
        val changingConfigurations =
            (owner as? AppCompatActivity)?.isChangingConfigurations ?: false
        if (!changingConfigurations) {
            isInBackground = true
            onBackgrounded()
        }
    }
}

private val FinancialConnectionsSheetNativeActivityArgs.theme: Theme
    get() = initialSyncResponse.manifest.theme?.toLocalTheme() ?: Theme.default

internal fun FinancialConnectionsSessionManifest.Theme.toLocalTheme(): Theme {
    return when (this) {
        FinancialConnectionsSessionManifest.Theme.LIGHT,
        FinancialConnectionsSessionManifest.Theme.DASHBOARD_LIGHT -> {
            Theme.DefaultLight
        }
        FinancialConnectionsSessionManifest.Theme.LINK_LIGHT -> {
            Theme.LinkLight
        }
    }
}
