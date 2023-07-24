package com.stripe.android.identity

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.annotation.VisibleForTesting
import androidx.appcompat.app.AlertDialog
import androidx.browser.customtabs.CustomTabsIntent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.NavController
import androidx.navigation.NavDestination
import com.stripe.android.camera.CameraPermissionCheckingActivity
import com.stripe.android.camera.framework.time.asEpochMillisecondsClockMark
import com.stripe.android.core.injection.IOContext
import com.stripe.android.core.injection.Injectable
import com.stripe.android.core.injection.Injector
import com.stripe.android.core.injection.UIContext
import com.stripe.android.core.injection.injectWithFallback
import com.stripe.android.identity.IdentityVerificationSheet.VerificationFlowResult
import com.stripe.android.identity.injection.DaggerIdentityActivityFallbackComponent
import com.stripe.android.identity.injection.IdentityActivitySubcomponent
import com.stripe.android.identity.navigation.ConfirmationDestination
import com.stripe.android.identity.navigation.ConsentDestination
import com.stripe.android.identity.navigation.ErrorDestination
import com.stripe.android.identity.navigation.ErrorDestination.Companion.ARG_SHOULD_FAIL
import com.stripe.android.identity.navigation.IdentityNavGraph
import com.stripe.android.identity.navigation.IndividualWelcomeDestination
import com.stripe.android.identity.navigation.navigateToFinalErrorScreen
import com.stripe.android.identity.ui.IdentityTheme
import com.stripe.android.identity.ui.IdentityTopBarState
import com.stripe.android.identity.viewmodel.IdentityViewModel
import javax.inject.Inject
import javax.inject.Provider
import kotlin.coroutines.CoroutineContext

/**
 * Host activity to perform Identity verification.
 */
internal class IdentityActivity :
    CameraPermissionCheckingActivity(),
    VerificationFlowFinishable,
    FallbackUrlLauncher,
    Injectable<Context> {
    @VisibleForTesting
    internal lateinit var navController: NavController

    private lateinit var onBackPressedCallback: IdentityOnBackPressedHandler

    @VisibleForTesting
    internal var viewModelFactory: ViewModelProvider.Factory =
        IdentityViewModel.IdentityViewModelFactory(
            { application },
            { uiContext },
            { workContext },
            { subcomponent }
        )

    private val starterArgs: IdentityVerificationSheetContract.Args by lazy {
        requireNotNull(IdentityVerificationSheetContract.Args.fromIntent(intent)) {
            EMPTY_ARG_ERROR
        }
    }

    private val identityViewModel: IdentityViewModel by viewModels { viewModelFactory }

    private lateinit var fallbackUrlLauncher: ActivityResultLauncher<Intent>

    lateinit var subcomponent: IdentityActivitySubcomponent

    @Inject
    lateinit var subComponentBuilderProvider: Provider<IdentityActivitySubcomponent.Builder>

    @Inject
    @UIContext
    lateinit var uiContext: CoroutineContext

    @Inject
    @IOContext
    lateinit var workContext: CoroutineContext

    override fun fallbackInitialize(arg: Context): Injector? {
        DaggerIdentityActivityFallbackComponent.builder()
            .context(arg)
            .build().inject(this)
        return null
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean(KEY_PRESENTED, true)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        injectWithFallback(
            starterArgs.injectorKey,
            this
        )
        subcomponent = subComponentBuilderProvider.get()
            .args(starterArgs)
            .cameraPermissionEnsureable(this)
            .appSettingsOpenable(this)
            .verificationFlowFinishable(this)
            .identityViewModelFactory(viewModelFactory)
            .fallbackUrlLauncher(this)
            .build()
        identityViewModel.retrieveAndBufferVerificationPage()
        identityViewModel.initializeTfLite()
        identityViewModel.registerActivityResultCaller(this)
        fallbackUrlLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) {
            identityViewModel.observeForVerificationPage(
                this,
                onSuccess = {
                    finishWithResult(
                        if (it.submitted) {
                            identityViewModel.sendAnalyticsRequest(
                                identityViewModel.identityAnalyticsRequestFactory
                                    .verificationSucceeded(
                                        isFromFallbackUrl = true
                                    )
                            )
                            VerificationFlowResult.Completed
                        } else {
                            identityViewModel.sendAnalyticsRequest(
                                identityViewModel.identityAnalyticsRequestFactory
                                    .verificationCanceled(
                                        isFromFallbackUrl = true
                                    )
                            )
                            VerificationFlowResult.Canceled
                        }
                    )
                },
                onFailure = {
                    identityViewModel.sendAnalyticsRequest(
                        identityViewModel.identityAnalyticsRequestFactory.verificationFailed(
                            isFromFallbackUrl = true,
                            throwable = IllegalStateException(it)
                        )
                    )
                    finishWithResult(VerificationFlowResult.Failed(IllegalStateException(it)))
                }
            )
        }

        identityViewModel.observeForVerificationPage(
            this,
            onSuccess = {
                if (savedInstanceState?.getBoolean(KEY_PRESENTED, false) != true) {
                    identityViewModel.sendAnalyticsRequest(
                        identityViewModel.identityAnalyticsRequestFactory.sheetPresented()
                    )
                }
            },
            onFailure = {
                identityViewModel.errorCause.postValue(it)
                navController.navigateToFinalErrorScreen(this)
            }
        )

        identityViewModel.screenTracker.screenTransitionStart(
            startedAt = starterArgs.presentTime.asEpochMillisecondsClockMark()
        )
        supportActionBar?.hide()

        setContent {
            var topBarState by remember {
                mutableStateOf(IdentityTopBarState.GO_BACK)
            }
            IdentityTheme {
                IdentityNavGraph(
                    identityViewModel = identityViewModel,
                    fallbackUrlLauncher = this,
                    appSettingsOpenable = this,
                    cameraPermissionEnsureable = this,
                    verificationFlowFinishable = this,
                    identityScanViewModelFactory = subcomponent.identityScanViewModelFactory,
                    topBarState = topBarState,
                    onTopBarNavigationClick = {
                        onBackPressedCallback.handleOnBackPressed()
                    },
                ) {
                    this.navController = it
                    onBackPressedCallback =
                        IdentityOnBackPressedHandler(
                            this,
                            this,
                            navController,
                            identityViewModel
                        )
                    onBackPressedDispatcher.addCallback(this, onBackPressedCallback)

                    navController.addOnDestinationChangedListener { _, destination, args ->
                        // Note: args is a Bundle created from arguments in route
                        onBackPressedCallback.updateState(destination, args)
                        topBarState = updateTopBarState(destination, args)
                    }
                }
            }
        }
    }

    override fun finishWithResult(result: VerificationFlowResult) {
        identityViewModel.sendAnalyticsRequest(
            identityViewModel.identityAnalyticsRequestFactory.sheetClosed(
                result.toString()
            )
        )
        setResult(
            Activity.RESULT_OK,
            Intent().putExtras(result.toBundle())
        )
        finish()
    }

    /**
     * Display the permission rational dialog without writing PERMISSION_RATIONALE_SHOWN, this would
     * prevent [showPermissionDeniedDialog] from being called and always trigger
     * [CameraPermissionCheckingActivity.requestCameraPermission].
     */
    override fun showPermissionRationaleDialog() {
        val builder = AlertDialog.Builder(this)
        builder.setMessage(R.string.stripe_camera_permission_rationale)
            .setPositiveButton(R.string.stripe_ok) { _, _ ->
                requestCameraPermission()
            }
        builder.show()
    }

    // This should have neve been invoked as PERMISSION_RATIONALE_SHOWN is never written.
    // Identity has its own CameraPermissionDeniedFragment to handle this case.
    override fun showPermissionDeniedDialog() {
        // no-op
    }

    private fun updateTopBarState(destination: NavDestination, args: Bundle?) =
        // Toggle the navigation button UI
        when (destination.route) {
            ConsentDestination.ROUTE.route -> {
                IdentityTopBarState.CLOSE
            }
            ConfirmationDestination.ROUTE.route -> {
                IdentityTopBarState.CLOSE
            }
            ErrorDestination.ROUTE.route -> {
                if (args?.getBoolean(ARG_SHOULD_FAIL, false) == true) {
                    IdentityTopBarState.CLOSE
                } else {
                    IdentityTopBarState.GO_BACK
                }
            }
            IndividualWelcomeDestination.ROUTE.route -> {
                IdentityTopBarState.CLOSE
            }
            else -> {
                IdentityTopBarState.GO_BACK
            }
        }

    override fun launchFallbackUrl(fallbackUrl: String) {
        val customTabsIntent = CustomTabsIntent.Builder()
            .build()
        customTabsIntent.intent.data = Uri.parse(fallbackUrl)
        fallbackUrlLauncher.launch(customTabsIntent.intent)
    }

    private companion object {
        const val EMPTY_ARG_ERROR =
            "IdentityActivity was started without arguments"

        const val KEY_PRESENTED = "presented"
    }
}
