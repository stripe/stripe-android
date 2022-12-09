package com.stripe.android.identity.navigation

import android.os.Bundle
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
import com.stripe.android.identity.IdentityVerificationSheet
import com.stripe.android.identity.VerificationFlowFinishable
import com.stripe.android.identity.analytics.IdentityAnalyticsRequestFactory.Companion.SCREEN_NAME_CONFIRMATION
import com.stripe.android.identity.networking.Resource
import com.stripe.android.identity.ui.ConfirmationScreen
import com.stripe.android.identity.viewmodel.IdentityViewModel
import kotlinx.coroutines.launch

/**
 * Fragment for confirmation.
 */
internal class ConfirmationFragment(
    private val identityViewModelFactory: ViewModelProvider.Factory,
    private val verificationFlowFinishable: VerificationFlowFinishable
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
            val verificationPage by identityViewModel.verificationPage.observeAsState(Resource.loading())
            ConfirmationScreen(
                verificationPageState = verificationPage,
                onError = {
                    identityViewModel.errorCause.postValue(it)
                    findNavController().navigateToErrorScreenWithDefaultValues(requireContext())
                },
                onComposeFinish = {
                    lifecycleScope.launch(identityViewModel.workContext) {
                        identityViewModel.screenTracker.screenTransitionFinish(
                            SCREEN_NAME_CONFIRMATION
                        )
                    }
                    identityViewModel.sendAnalyticsRequest(
                        identityViewModel.identityAnalyticsRequestFactory.screenPresented(
                            screenName = SCREEN_NAME_CONFIRMATION
                        )
                    )
                }
            ) {
                identityViewModel.sendSucceededAnalyticsRequestForNative()
                verificationFlowFinishable.finishWithResult(
                    IdentityVerificationSheet.VerificationFlowResult.Completed
                )
            }
        }
    }
}
