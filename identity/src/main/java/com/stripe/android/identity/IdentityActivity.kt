package com.stripe.android.identity

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.annotation.VisibleForTesting
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.content.res.AppCompatResources
import androidx.browser.customtabs.CustomTabsIntent
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.NavController
import androidx.navigation.NavDestination
import androidx.navigation.fragment.NavHostFragment
import com.google.android.material.appbar.MaterialToolbar
import com.stripe.android.camera.CameraPermissionCheckingActivity
import com.stripe.android.camera.framework.time.asEpochMillisecondsClockMark
import com.stripe.android.core.injection.IOContext
import com.stripe.android.core.injection.Injectable
import com.stripe.android.core.injection.UIContext
import com.stripe.android.core.injection.injectWithFallback
import com.stripe.android.identity.IdentityVerificationSheet.VerificationFlowResult
import com.stripe.android.identity.analytics.IdentityAnalyticsRequestFactory.Companion.SCREEN_NAME_CONSENT
import com.stripe.android.identity.databinding.IdentityActivityBinding
import com.stripe.android.identity.injection.DaggerIdentityActivityFallbackComponent
import com.stripe.android.identity.injection.IdentityActivitySubcomponent
import com.stripe.android.identity.navigation.ErrorFragment
import com.stripe.android.identity.networking.models.VerificationPage.Companion.requireSelfie
import com.stripe.android.identity.utils.navigateUpAndSetArgForUploadFragment
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

    @VisibleForTesting
    internal var viewModelFactory: ViewModelProvider.Factory =
        IdentityViewModel.IdentityViewModelFactory(
            this,
            { uiContext },
            { workContext },
            { subcomponent }
        )

    private val binding by lazy {
        IdentityActivityBinding.inflate(layoutInflater)
    }

    private val starterArgs: IdentityVerificationSheetContract.Args by lazy {
        requireNotNull(IdentityVerificationSheetContract.Args.fromIntent(intent)) {
            EMPTY_ARG_ERROR
        }
    }

    private val identityViewModel: IdentityViewModel by viewModels { viewModelFactory }

    private val onBackPressedCallback by lazy {
        IdentityActivityOnBackPressedCallback(
            this,
            navController,
            identityViewModel
        )
    }
    private lateinit var fallbackUrlLauncher: ActivityResultLauncher<Intent>

    private var launchedFallbackUrl: Boolean = false

    lateinit var subcomponent: IdentityActivitySubcomponent

    @Inject
    lateinit var subComponentBuilderProvider: Provider<IdentityActivitySubcomponent.Builder>

    @Inject
    @UIContext
    lateinit var uiContext: CoroutineContext

    @Inject
    @IOContext
    lateinit var workContext: CoroutineContext

    override fun fallbackInitialize(arg: Context) {
        DaggerIdentityActivityFallbackComponent.builder()
            .context(arg)
            .build().inject(this)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean(KEY_LAUNCHED_FALLBACK_URL, launchedFallbackUrl)
        outState.putBoolean(KEY_PRESENTED, true)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        injectWithFallback(
            starterArgs.injectorKey,
            this
        )
        subcomponent = subComponentBuilderProvider.get()
            .args(starterArgs)
            .cameraPermissionEnsureable(this)
            .appSettingsOpenable(this)
            .verificationFlowFinishable(this)
            .identityViewModelFactory(viewModelFactory as IdentityViewModel.IdentityViewModelFactory)
            .fallbackUrlLauncher(this)
            .build()

        supportFragmentManager.fragmentFactory = subcomponent.identityFragmentFactory

        super.onCreate(savedInstanceState)
        fallbackUrlLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) {
            identityViewModel.retrieveAndBufferVerificationPage()
            identityViewModel.observeForVerificationPage(
                this,
                onSuccess = {
                    finishWithResult(
                        if (it.submitted) {
                            identityViewModel.sendAnalyticsRequest(
                                identityViewModel.identityAnalyticsRequestFactory.verificationSucceeded(
                                    isFromFallbackUrl = true
                                )
                            )
                            VerificationFlowResult.Completed
                        } else {
                            identityViewModel.sendAnalyticsRequest(
                                identityViewModel.identityAnalyticsRequestFactory.verificationCanceled(
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

        if (savedInstanceState?.getBoolean(KEY_PRESENTED, false) != true) {
            identityViewModel.sendAnalyticsRequest(
                identityViewModel.identityAnalyticsRequestFactory.sheetPresented()
            )
        }

        identityViewModel.screenTracker.screenTransitionStart(
            startedAt = starterArgs.presentTime.asEpochMillisecondsClockMark()
        )

        if (savedInstanceState == null || !savedInstanceState.getBoolean(
                KEY_LAUNCHED_FALLBACK_URL,
                false
            )
        ) {
            // The Activity is newly created, set up navigation flow normally
            setContentView(binding.root)
            setUpNavigationController()
            identityViewModel.retrieveAndBufferVerificationPage()
        } else {
            // The Activity is being recreated after being destroyed by OS.
            // This happens when a fallback URL Activity is in front and IdentityActivity is destroyed.
            // In this case, remove the NavHostFragment set earlier and let fallbackUrlLauncher return
            // the callback to client.

            // Recovered activity should already set up supportFragmentManager with a single NavHostFragment
            require(supportFragmentManager.fragments.size == 1) {
                "supportFragmentManager contains more than one fragment"
            }
            supportFragmentManager.beginTransaction().remove(supportFragmentManager.fragments[0])
                .commit()
        }
    }

    private fun setUpNavigationController() {
        // hide supportActionBar and use the customized ToolBar to configure NavController.
        // supportActionBar is unreliable as it might be null if host app uses a NoActionBar theme.
        supportActionBar?.hide()

        navController =
            (supportFragmentManager.findFragmentById(R.id.identity_nav_host) as NavHostFragment).navController

        navController.setGraph(R.navigation.identity_nav_graph)

        onBackPressedDispatcher.addCallback(this, onBackPressedCallback)

        navController.addOnDestinationChangedListener { _, destination, args ->
            onBackPressedCallback.updateState(destination, args)
            binding.topAppBar.updateState(destination, args)
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
        builder.setMessage(R.string.camera_permission_rationale)
            .setPositiveButton(R.string.ok) { _, _ ->
                requestCameraPermission()
            }
        builder.show()
    }

    // This should have neve been invoked as PERMISSION_RATIONALE_SHOWN is never written.
    // Identity has its own CameraPermissionDeniedFragment to handle this case.
    override fun showPermissionDeniedDialog() {
        // no-op
    }

    /**
     * Handles Toolbar's navigation button behavior based on current navigation status.
     */
    private fun MaterialToolbar.updateState(destination: NavDestination, args: Bundle?) {
        when {
            // Display cross icon on consent fragment, clicking it finishes the flow with Canceled
            isConsentFragment(destination) -> {
                this.navigationIcon =
                    AppCompatResources.getDrawable(
                        this@IdentityActivity,
                        R.drawable.ic_baseline_close_24
                    )
                this.setNavigationOnClickListener {
                    identityViewModel.sendAnalyticsRequest(
                        identityViewModel.identityAnalyticsRequestFactory.verificationCanceled(
                            isFromFallbackUrl = false,
                            lastScreenName = SCREEN_NAME_CONSENT,
                            requireSelfie = identityViewModel.verificationPage.value?.data?.requireSelfie()
                        )
                    )
                    finishWithResult(
                        VerificationFlowResult.Canceled
                    )
                }
            }
            // Display cross icon on error fragment that should fail, clicking it finishes the flow with Failed
            isErrorFragmentThatShouldFail(destination, args) -> {
                this.navigationIcon = AppCompatResources.getDrawable(
                    this@IdentityActivity,
                    R.drawable.ic_baseline_close_24
                )
                this.setNavigationOnClickListener {
                    val failedReason = requireNotNull(
                        args?.getSerializable(
                            ErrorFragment.ARG_CAUSE
                        ) as? Throwable
                    ) {
                        "Failed to get failedReason from $args"
                    }

                    identityViewModel.sendAnalyticsRequest(
                        identityViewModel.identityAnalyticsRequestFactory.verificationFailed(
                            isFromFallbackUrl = false,
                            requireSelfie = identityViewModel.verificationPage.value?.data?.requireSelfie(),
                            throwable = failedReason
                        )
                    )
                    finishWithResult(
                        VerificationFlowResult.Failed(failedReason)
                    )
                }
            }
            // Otherwise display back arrow icon, clicking it navigates up
            else -> {
                this.navigationIcon =
                    AppCompatResources.getDrawable(
                        this@IdentityActivity,
                        R.drawable.ic_baseline_arrow_back_24
                    )
                this.setNavigationOnClickListener {
                    navController.navigateUpAndSetArgForUploadFragment()
                }
            }
        }
    }

    /**
     * Handles back button behavior based on current navigation status.
     */
    private class IdentityActivityOnBackPressedCallback(
        private val verificationFlowFinishable: VerificationFlowFinishable,
        private val navController: NavController,
        private val identityViewModel: IdentityViewModel
    ) : OnBackPressedCallback(true) {
        private var destination: NavDestination? = null
        private var args: Bundle? = null

        fun updateState(destination: NavDestination, args: Bundle?) {
            this.destination = destination
            this.args = args
        }

        override fun handleOnBackPressed() {
            when {
                // On consent fragment, clicking back finishes the flow with Canceled
                isConsentFragment(destination) -> {
                    identityViewModel.sendAnalyticsRequest(
                        identityViewModel.identityAnalyticsRequestFactory.verificationCanceled(
                            isFromFallbackUrl = false,
                            lastScreenName = SCREEN_NAME_CONSENT,
                            requireSelfie = identityViewModel.verificationPage.value?.data?.requireSelfie()
                        )
                    )
                    verificationFlowFinishable.finishWithResult(
                        VerificationFlowResult.Canceled
                    )
                }
                // On error fragment that should fail, clicking back finishes the flow with Failed
                isErrorFragmentThatShouldFail(destination, args) -> {
                    val failedReason = requireNotNull(
                        args?.getSerializable(
                            ErrorFragment.ARG_CAUSE
                        ) as? Throwable
                    ) {
                        "Failed to get failedReason from $args"
                    }
                    identityViewModel.sendAnalyticsRequest(
                        identityViewModel.identityAnalyticsRequestFactory.verificationFailed(
                            isFromFallbackUrl = false,
                            requireSelfie = identityViewModel.verificationPage.value?.data?.requireSelfie(),
                            throwable = failedReason
                        )
                    )
                    verificationFlowFinishable.finishWithResult(
                        VerificationFlowResult.Failed(failedReason)
                    )
                }
                // On other fragments, clicking back navigates up
                else -> {
                    navController.navigateUpAndSetArgForUploadFragment()
                }
            }
        }
    }

    override fun launchFallbackUrl(fallbackUrl: String) {
        launchedFallbackUrl = true
        val customTabsIntent = CustomTabsIntent.Builder()
            .build()
        customTabsIntent.intent.data = Uri.parse(fallbackUrl)
        fallbackUrlLauncher.launch(customTabsIntent.intent)
    }

    private companion object {
        const val EMPTY_ARG_ERROR =
            "IdentityActivity was started without arguments"

        const val KEY_LAUNCHED_FALLBACK_URL = "launched_fallback_url"

        const val KEY_PRESENTED = "presented"

        private fun isConsentFragment(destination: NavDestination?) =
            destination?.id == R.id.consentFragment

        /**
         * Check if this is the final error fragment, which would fail the verification flow when
         * back button is clicked.
         */
        private fun isErrorFragmentThatShouldFail(
            destination: NavDestination?,
            args: Bundle?
        ) = destination?.id == R.id.errorFragment &&
            args?.getBoolean(ErrorFragment.ARG_SHOULD_FAIL, false) == true
    }
}
