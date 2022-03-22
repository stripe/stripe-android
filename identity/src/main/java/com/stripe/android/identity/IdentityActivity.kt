package com.stripe.android.identity

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import androidx.annotation.VisibleForTesting
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.NavController
import androidx.navigation.findNavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupActionBarWithNavController
import com.stripe.android.camera.CameraPermissionCheckingActivity
import com.stripe.android.identity.IdentityVerificationSheet.VerificationResult
import com.stripe.android.identity.databinding.IdentityActivityBinding
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        setSupportActionBar(binding.topAppBar)

        supportFragmentManager.fragmentFactory = identityFragmentFactory

        navController =
            (supportFragmentManager.findFragmentById(R.id.identity_nav_host) as NavHostFragment).navController
        navController.setGraph(R.navigation.identity_nav_graph)
        navController.addOnDestinationChangedListener { _, _, _ ->
            title = "" // clear title on each screen
        }
        setupActionBarWithNavController(navController)

        identityViewModel.retrieveAndBufferVerificationPage()
    }

    override fun onBackPressed() {
        findNavController(R.id.identity_nav_host).let { navController ->
            if (navController.currentDestination?.id == R.id.consentFragment) {
                finishWithResult(VerificationResult.Canceled)
            } else {
                navController.navigateUp()
            }
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        return navController.navigateUp() || super.onSupportNavigateUp()
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
