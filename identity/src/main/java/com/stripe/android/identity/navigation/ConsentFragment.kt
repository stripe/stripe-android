package com.stripe.android.identity.navigation

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.stripe.android.identity.FallbackUrlLauncher
import com.stripe.android.identity.R
import com.stripe.android.identity.analytics.IdentityAnalyticsRequestFactory.Companion.SCREEN_NAME_CONSENT
import com.stripe.android.identity.networking.Resource
import com.stripe.android.identity.networking.models.ClearDataParam
import com.stripe.android.identity.networking.models.CollectedDataParam
import com.stripe.android.identity.networking.models.VerificationPage.Companion.requireSelfie
import com.stripe.android.identity.ui.ConsentScreen
import com.stripe.android.identity.utils.navigateToErrorFragmentWithFailedReason
import com.stripe.android.identity.utils.postVerificationPageDataAndMaybeSubmit
import com.stripe.android.identity.viewmodel.IdentityViewModel
import kotlinx.coroutines.launch

/**
 * The start screen of Identification flow, prompt for client's consent.
 *
 */
internal class ConsentFragment(
    private val identityViewModelFactory: ViewModelProvider.Factory,
    private val fallbackUrlLauncher: FallbackUrlLauncher
) : Fragment() {
    private val identityViewModel: IdentityViewModel by activityViewModels {
        identityViewModelFactory
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ) = ComposeView(requireContext()).apply {
        setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
        setContent {
            val verificationState by identityViewModel.verificationPage.observeAsState(Resource.loading())

            ConsentScreen(
                merchantLogoUri = identityViewModel.verificationArgs.brandLogo,
                verificationState = verificationState,
                onSuccess = { verificationPage ->
                    identityViewModel.updateAnalyticsState { oldState ->
                        oldState.copy(
                            requireSelfie = verificationPage.requireSelfie()
                        )
                    }
                },
                onFallbackUrl = this@ConsentFragment::logErrorAndLaunchFallback,
                onError = this@ConsentFragment::logErrorAndNavigateToError,
                onConsentAgreed = this@ConsentFragment::agreeConsentAndPost,
                onConsentDeclined = this@ConsentFragment::declineConsentAndPost
            )
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        lifecycleScope.launch(identityViewModel.workContext) {
            identityViewModel.screenTracker.screenTransitionFinish(SCREEN_NAME_CONSENT)
        }

        identityViewModel.sendAnalyticsRequest(
            identityViewModel.identityAnalyticsRequestFactory.screenPresented(
                screenName = SCREEN_NAME_CONSENT
            )
        )
    }

    private fun logErrorAndLaunchFallback(fallbackUrl: String) {
        Log.e(TAG, "Unsupported client, launching fallback url")
        fallbackUrlLauncher.launchFallbackUrl(fallbackUrl)
    }

    private fun logErrorAndNavigateToError(throwable: Throwable) {
        Log.e(
            TAG,
            "Failed to get verificationPage: $throwable"
        )
        navigateToErrorFragmentWithFailedReason(throwable)
    }

    private fun agreeConsentAndPost(requireSelfie: Boolean) {
        postVerificationPageDataAndNavigate(
            CollectedDataParam(
                biometricConsent = true
            ),
            requireSelfie
        )
    }

    private fun declineConsentAndPost(requireSelfie: Boolean) {
        postVerificationPageDataAndNavigate(
            CollectedDataParam(
                false
            ),
            requireSelfie
        )
    }

    /**
     * Post VerificationPageData with the type and navigate base on its result.
     */
    private fun postVerificationPageDataAndNavigate(
        collectedDataParam: CollectedDataParam,
        requireSelfie: Boolean
    ) {
        lifecycleScope.launch {
            postVerificationPageDataAndMaybeSubmit(
                identityViewModel,
                collectedDataParam,
                if (requireSelfie)
                    ClearDataParam.CONSENT_TO_DOC_SELECT_WITH_SELFIE
                else
                    ClearDataParam.CONSENT_TO_DOC_SELECT,
                fromFragment = R.id.consentFragment,
                notSubmitBlock = {
                    findNavController().navigate(R.id.action_consentFragment_to_docSelectionFragment)
                }
            )
        }
    }

    private companion object {
        val TAG: String = ConsentFragment::class.java.simpleName
    }
}
