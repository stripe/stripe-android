package com.stripe.android.identity.navigation

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.stripe.android.identity.IdentityVerificationSheet
import com.stripe.android.identity.VerificationFlowFinishable
import com.stripe.android.identity.analytics.IdentityAnalyticsRequestFactory.Companion.SCREEN_NAME_CONFIRMATION
import com.stripe.android.identity.databinding.ConfirmationFragmentBinding
import com.stripe.android.identity.utils.navigateToDefaultErrorFragment
import com.stripe.android.identity.utils.setHtmlString
import com.stripe.android.identity.viewmodel.IdentityViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
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

    private lateinit var binding: ConfirmationFragmentBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = ConfirmationFragmentBinding.inflate(inflater, container, false)
        binding.kontinue.setOnClickListener {
            lifecycleScope.launch {
                identityViewModel.analyticsState.collectLatest {
                    identityViewModel.sendAnalyticsRequest(
                        identityViewModel.identityAnalyticsRequestFactory.verificationSucceeded(
                            isFromFallbackUrl = false,
                            scanType = it.scanType,
                            requireSelfie = it.requireSelfie,
                            docFrontRetryTimes = it.docFrontRetryTimes,
                            docBackRetryTimes = it.docBackRetryTimes,
                            selfieRetryTimes = it.selfieRetryTimes,
                            docFrontUploadType = it.docFrontUploadType,
                            docBackUploadType = it.docBackUploadType,
                            docFrontModelScore = it.docFrontModelScore,
                            docBackModelScore = it.docBackModelScore,
                            selfieModelScore = it.selfieModelScore
                        )
                    )
                }
            }
            identityViewModel.sendSucceededAnalyticsRequestForNative()
            verificationFlowFinishable.finishWithResult(
                IdentityVerificationSheet.VerificationFlowResult.Completed
            )
        }
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        identityViewModel.observeForVerificationPage(
            viewLifecycleOwner,
            onSuccess = { verificationPage ->
                binding.titleText.text = verificationPage.success.title
                binding.contentText.setHtmlString(verificationPage.success.body)
                binding.kontinue.text = verificationPage.success.buttonText
            },
            onFailure = {
                Log.e(TAG, "Failed to get VerificationPage")
                navigateToDefaultErrorFragment()
            }
        )

        lifecycleScope.launch(Dispatchers.IO) {
            identityViewModel.screenTracker.screenTransitionFinish(SCREEN_NAME_CONFIRMATION)
        }

        identityViewModel.sendAnalyticsRequest(
            identityViewModel.identityAnalyticsRequestFactory.screenPresented(
                screenName = SCREEN_NAME_CONFIRMATION
            )
        )
    }

    private companion object {
        val TAG: String = ConfirmationFragment::class.java.simpleName
    }
}
