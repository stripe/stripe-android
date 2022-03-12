package com.stripe.android.identity

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import androidx.annotation.VisibleForTesting
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.findNavController
import androidx.navigation.fragment.NavHostFragment
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

    @VisibleForTesting
    internal val viewModelFactory: ViewModelProvider.Factory by lazy {
        identityFragmentFactory.identityViewModelFactory
    }

    @VisibleForTesting
    internal val identityViewModel: IdentityViewModel by viewModels { viewModelFactory }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        supportFragmentManager.fragmentFactory = identityFragmentFactory

        val navHostFragment =
            supportFragmentManager.findFragmentById(R.id.identity_nav_host) as NavHostFragment
        navHostFragment.navController.setGraph(R.navigation.identity_nav_graph)

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

    override fun finishWithResult(result: VerificationResult) {
        setResult(
            Activity.RESULT_OK,
            Intent().putExtras(result.toBundle())
        )
        finish()
    }

    private companion object {
        const val EMPTY_ARG_ERROR =
            "IdentityActivity was started without arguments"
    }
}
