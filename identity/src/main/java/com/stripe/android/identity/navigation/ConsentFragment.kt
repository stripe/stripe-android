package com.stripe.android.identity.navigation

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.stripe.android.identity.IdentityVerificationSheet
import com.stripe.android.identity.R
import com.stripe.android.identity.VerificationFlowFinishable
import com.stripe.android.identity.databinding.ConsentFragmentBinding
import com.stripe.android.identity.networking.models.ClearDataParam
import com.stripe.android.identity.networking.models.CollectedDataParam
import com.stripe.android.identity.networking.models.ConsentParam
import com.stripe.android.identity.networking.models.VerificationPage.Companion.isMissingBiometricConsent
import com.stripe.android.identity.networking.models.VerificationPageData.Companion.isMissingDocumentType
import com.stripe.android.identity.networking.models.VerificationPageStaticContentConsentPage
import com.stripe.android.identity.utils.navigateToErrorFragmentWithFailedReason
import com.stripe.android.identity.utils.postVerificationPageDataAndMaybeSubmit
import com.stripe.android.identity.utils.setHtmlString
import com.stripe.android.identity.viewmodel.IdentityViewModel
import kotlinx.coroutines.launch

/**
 * The start screen of Identification flow, prompt for client's consent.
 *
 */
internal class ConsentFragment(
    private val identityViewModelFactory: ViewModelProvider.Factory,
    private val verificationFlowFinishable: VerificationFlowFinishable,
) : Fragment() {
    private lateinit var binding: ConsentFragmentBinding

    private val identityViewModel: IdentityViewModel by activityViewModels {
        identityViewModelFactory
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requireActivity().onBackPressedDispatcher.addCallback(
            this,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    verificationFlowFinishable.finishWithResult(
                        IdentityVerificationSheet.VerificationResult.Canceled
                    )
                }
            }
        )
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = ConsentFragmentBinding.inflate(inflater, container, false)

        Glide.with(requireContext()).load(identityViewModel.args.brandLogo)
            .into(binding.merchantLogo)

        binding.agree.setOnClickListener {
            binding.agree.toggleToLoading()
            binding.decline.isEnabled = false
            postVerificationPageDataAndNavigate(
                CollectedDataParam(
                    consent = ConsentParam(biometric = true)
                )
            )
        }
        binding.decline.setOnClickListener {
            binding.decline.toggleToLoading()
            binding.agree.isEnabled = false
            postVerificationPageDataAndNavigate(
                CollectedDataParam(
                    consent = ConsentParam(biometric = false)
                )
            )
        }
        binding.progressCircular.setOnClickListener {
            setLoadingFinishedUI()
        }
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        identityViewModel.observeForVerificationPage(
            viewLifecycleOwner,
            onSuccess = { verificationPage ->
                if (verificationPage.isMissingBiometricConsent()) {
                    setLoadingFinishedUI()
                    bindViewData(verificationPage.biometricConsent)
                } else {
                    navigateToDocSelection()
                }
            },
            onFailure = {
                Log.e(TAG, "Failed to get verificationPage: $it")
                // TODO(ccen) parse the error message from Status.ERROR
                navigateToErrorFragmentWithFailedReason(
                    it ?: IllegalStateException("Failed to get verificationPage")
                )
            }
        )
    }

    /**
     * Post VerificationPageData with the type and navigate base on its result.
     */
    private fun postVerificationPageDataAndNavigate(collectedDataParam: CollectedDataParam) {
        lifecycleScope.launch {
            postVerificationPageDataAndMaybeSubmit(
                identityViewModel,
                collectedDataParam,
                ClearDataParam.CONSENT_TO_DOC_SELECT,
                shouldNotSubmit = { true },
                notSubmitBlock = { verificationPageData ->
                    if (verificationPageData.isMissingDocumentType()) {
                        navigateToDocSelection()
                    } else {
                        // TODO(ccen) Determine the behavior when verificationPageData.isMissingDocumentType() is false
                        // how to get the type that's already selected
                    }
                }
            )
        }
    }

    private fun navigateToDocSelection() {
        findNavController().navigate(R.id.action_consentFragment_to_docSelectionFragment)
    }

    private fun bindViewData(consentPage: VerificationPageStaticContentConsentPage) {
        binding.titleText.text = consentPage.title
        binding.privacyPolicy.setHtmlString(consentPage.privacyPolicy)
        binding.timeEstimate.text = consentPage.timeEstimate
        binding.body.setHtmlString(consentPage.body)
        binding.agree.setText(consentPage.acceptButtonText)
        binding.decline.setText(consentPage.declineButtonText)
    }

    private fun setLoadingFinishedUI() {
        binding.loadings.visibility = View.GONE
        binding.texts.visibility = View.VISIBLE
        binding.buttons.visibility = View.VISIBLE
    }

    private companion object {
        val TAG: String = ConsentFragment::class.java.simpleName
    }
}
