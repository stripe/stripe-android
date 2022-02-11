package com.stripe.android.identity

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import androidx.annotation.VisibleForTesting
import androidx.core.os.bundleOf
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.NavHostFragment
import com.stripe.android.camera.CameraPermissionCheckingActivity
import com.stripe.android.identity.IdentityVerificationSheet.VerificationResult
import com.stripe.android.identity.databinding.IdentityActivityBinding
import com.stripe.android.identity.navigation.ConsentFragment

/**
 * Host activity to perform Identity verification.
 */
internal class IdentityActivity : CameraPermissionCheckingActivity() {
    private val binding by lazy {
        IdentityActivityBinding.inflate(layoutInflater)
    }

    private val starterArgs: IdentityVerificationSheetContract.Args by lazy {
        requireNotNull(IdentityVerificationSheetContract.Args.fromIntent(intent)) {
            EMPTY_ARG_ERROR
        }
    }

    @VisibleForTesting
    internal val viewModelFactory: ViewModelProvider.Factory by lazy {
        IdentityViewModel.IdentityViewModelFactory(
            starterArgs
        )
    }

    @VisibleForTesting
    internal val viewModel: IdentityViewModel by viewModels { viewModelFactory }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        val navHostFragment =
            supportFragmentManager.findFragmentById(R.id.identity_nav_host) as NavHostFragment
        navHostFragment.navController.setGraph(
            R.navigation.identity_nav_graph,
            bundleOf(
                ConsentFragment.ARG_CONSENT_CONTEXT to "This is some context string that tells user how stripe will" +
                    " verify their identity. It could be in html format. It's sent from server",
                ConsentFragment.ARG_MERCHANT_LOGO to starterArgs.merchantLogo
            )
        )
    }

    override fun onBackPressed() {
        finishWithResult(VerificationResult.Canceled)
    }

    private fun finishWithResult(result: VerificationResult) {
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
