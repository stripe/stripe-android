package com.stripe.android.identity

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.activity.OnBackPressedCallback
import androidx.activity.viewModels
import androidx.annotation.VisibleForTesting
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.NavController
import androidx.navigation.NavDestination
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupWithNavController
import com.stripe.android.camera.CameraPermissionCheckingActivity
import com.stripe.android.identity.IdentityVerificationSheet.VerificationResult
import com.stripe.android.identity.databinding.IdentityActivityBinding
import com.stripe.android.identity.navigation.ErrorFragment
import com.stripe.android.identity.navigation.IdentityFragmentFactory
import com.stripe.android.identity.viewmodel.IdentityViewModel

/**
 * Host activity to perform Identity verification.
 */
internal class IdentityActivity : CameraPermissionCheckingActivity(), VerificationFlowFinishable {
    private val binding by lazy {
        IdentityActivityBinding.inflate(layoutInflater)
    }

    private val starterArgs: IdentityVerificationSheetContract.Args by lazy {
        requireNotNull(IdentityVerificationSheetContract.Args.fromIntent(intent)) {
            EMPTY_ARG_ERROR
        }
    }

    private val identityFragmentFactory: IdentityFragmentFactory by lazy {
        IdentityFragmentFactory(
            this,
            this,
            this,
            starterArgs,
            this
        )
    }

    private lateinit var navController: NavController

    @VisibleForTesting
    internal val viewModelFactory: ViewModelProvider.Factory by lazy {
        identityFragmentFactory.identityViewModelFactory
    }

    @VisibleForTesting
    internal val identityViewModel: IdentityViewModel by viewModels { viewModelFactory }

    private val onBackPressedCallback = object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
            navController.navigateUp()
        }
    }

    private fun isConsentFragment(destination: NavDestination) =
        destination.id == R.id.consentFragment

    private fun isErrorFragmentWithFailedReason(
        destination: NavDestination,
        args: Bundle?
    ) = destination.id == R.id.errorFragment &&
        args?.containsKey(ErrorFragment.ARG_FAILED_REASON) == true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        supportFragmentManager.fragmentFactory = identityFragmentFactory
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

        onBackPressedDispatcher.addCallback(onBackPressedCallback)
        navController.addOnDestinationChangedListener { _, destination, args ->
            // By default clicking back is the same as clicking navigate up.
            // When currently destination is ConsentFragment or ErrorFragment, the back press
            // behavior will be handled in the Fragment itself.
            onBackPressedCallback.isEnabled =
                !isConsentFragment(destination) &&
                !isErrorFragmentWithFailedReason(destination, args)
        }
        binding.topAppBar.setupWithNavController(
            navController,
            AppBarConfiguration(
                // navController.navigateUp() won't work on the two fragments because -
                //  consentFragment - it's the very first fragment of the navigation graph
                //  errorFragment - it's sometimes triggered by an error that needs to terminate
                //    verification flow(e.g incorrect network response). navigateUp would re-trigger
                //    the same error and let navController re-navigate to the errorFragment,
                //    creating a endless loop.
                //
                // Since we can't override the behavior of navController.navigateUp(), when
                // navigationDestination is on these two fragment, disable the up button to
                // prevent navController.navigateUp() being called.
                //
                // Note: system back button can be still pressed on these two fragments, they have
                // corresponding logic to end verification flow in different ways.
                topLevelDestinationIds = setOf(
                    R.id.consentFragment,
                    R.id.errorFragment
                )
            )
        )
    }

    override fun finishWithResult(result: VerificationResult) {
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

    private companion object {
        const val EMPTY_ARG_ERROR =
            "IdentityActivity was started without arguments"
    }
}
