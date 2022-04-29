package com.stripe.android.identity

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.activity.OnBackPressedCallback
import androidx.activity.viewModels
import androidx.annotation.VisibleForTesting
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.content.res.AppCompatResources
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.NavController
import androidx.navigation.NavDestination
import androidx.navigation.fragment.NavHostFragment
import com.google.android.material.appbar.MaterialToolbar
import com.stripe.android.camera.CameraPermissionCheckingActivity
import com.stripe.android.identity.IdentityVerificationSheet.VerificationFlowResult
import com.stripe.android.identity.databinding.IdentityActivityBinding
import com.stripe.android.identity.navigation.ErrorFragment
import com.stripe.android.identity.utils.navigateUpAndSetArgForUploadFragment
import com.stripe.android.identity.viewmodel.IdentityViewModel

/**
 * Host activity to perform Identity verification.
 */
internal class IdentityActivity : CameraPermissionCheckingActivity(), VerificationFlowFinishable {
    @VisibleForTesting
    internal lateinit var navController: NavController

    @VisibleForTesting
    internal var viewModelFactory: ViewModelProvider.Factory =
        IdentityViewModel.IdentityViewModelFactory(
            this,
            { starterArgs },
            this,
            this,
            this
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
            navController
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        supportFragmentManager.fragmentFactory = identityViewModel.identityFragmentFactory
        setUpNavigationController()
        identityViewModel.retrieveAndBufferVerificationPage()
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
                    finishWithResult(
                        VerificationFlowResult.Canceled
                    )
                }
            }
            // Display cross icon on error fragment with failed reason, clicking it finishes the flow with Failed
            isErrorFragmentWithFailedReason(destination, args) -> {
                this.navigationIcon = AppCompatResources.getDrawable(
                    this@IdentityActivity,
                    R.drawable.ic_baseline_close_24
                )
                this.setNavigationOnClickListener {
                    finishWithResult(
                        VerificationFlowResult.Failed(
                            requireNotNull(
                                args?.getSerializable(
                                    ErrorFragment.ARG_FAILED_REASON
                                ) as? Throwable
                            ) {
                                "Failed to get failedReason from $args"
                            }
                        )
                    )
                }
            }
            // Otherwise display back arrow icon, cliking it navigates up
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
        private val navController: NavController
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
                    verificationFlowFinishable.finishWithResult(
                        VerificationFlowResult.Canceled
                    )
                }
                // On error fragment with failed reason, clicking back finishes the flow with Failed
                isErrorFragmentWithFailedReason(destination, args) -> {
                    verificationFlowFinishable.finishWithResult(
                        VerificationFlowResult.Failed(
                            requireNotNull(
                                args?.getSerializable(
                                    ErrorFragment.ARG_FAILED_REASON
                                ) as? Throwable
                            ) {
                                "Failed to get failedReason from $args"
                            }
                        )
                    )
                }
                // On other fragments, clicking back navigates up
                else -> {
                    navController.navigateUpAndSetArgForUploadFragment()
                }
            }
        }
    }

    private companion object {
        const val EMPTY_ARG_ERROR =
            "IdentityActivity was started without arguments"

        private fun isConsentFragment(destination: NavDestination?) =
            destination?.id == R.id.consentFragment

        private fun isErrorFragmentWithFailedReason(
            destination: NavDestination?,
            args: Bundle?
        ) = destination?.id == R.id.errorFragment &&
            args?.containsKey(ErrorFragment.ARG_FAILED_REASON) == true
    }
}
