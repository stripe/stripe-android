package com.stripe.android.identity.navigation

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
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
import com.stripe.android.identity.analytics.IdentityAnalyticsRequestFactory.Companion.SCREEN_NAME_CONSENT
import com.stripe.android.identity.networking.Resource
import com.stripe.android.identity.networking.models.CollectedDataParam
import com.stripe.android.identity.networking.models.VerificationPage.Companion.requireSelfie
import com.stripe.android.identity.ui.ConsentScreen
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
                onComposeFinish = { verificationPage ->
                    identityViewModel.updateAnalyticsState { oldState ->
                        oldState.copy(
                            requireSelfie = verificationPage.requireSelfie()
                        )
                    }
                    lifecycleScope.launch(identityViewModel.workContext) {
                        identityViewModel.screenTracker.screenTransitionFinish(SCREEN_NAME_CONSENT)
                    }

                    identityViewModel.sendAnalyticsRequest(
                        identityViewModel.identityAnalyticsRequestFactory.screenPresented(
                            screenName = SCREEN_NAME_CONSENT
                        )
                    )
                },
                onFallbackUrl =
                { fallbackUrl ->
                    Log.e(TAG, "Unsupported client, launching fallback url")
                    fallbackUrlLauncher.launchFallbackUrl(fallbackUrl)
                },
                onError = { throwable ->
                    Log.e(
                        TAG,
                        "Failed to get verificationPage: $throwable"
                    )
                    identityViewModel.errorCause.postValue(throwable)
                    findNavController().navigateToErrorScreenWithFailedReason(
                        requireContext(),
                    )
                },
                onConsentAgreed =
                {
                    identityViewModel.postVerificationPageDataAndMaybeNavigate(
                        findNavController(),
                        CollectedDataParam(
                            biometricConsent = true
                        ),
                        ConsentDestination.ROUTE.route
                    )
                },
                onConsentDeclined = {
                    identityViewModel.postVerificationPageDataAndMaybeNavigate(
                        findNavController(),
                        CollectedDataParam(
                            biometricConsent = false
                        ),
                        ConsentDestination.ROUTE.route
                    )
                }
            )
        }
    }

    private companion object {
        val TAG: String = ConsentFragment::class.java.simpleName
    }
}
