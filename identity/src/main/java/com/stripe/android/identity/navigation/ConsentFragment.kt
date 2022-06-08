package com.stripe.android.identity.navigation

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.stripe.android.identity.FallbackUrlLauncher
import com.stripe.android.identity.R
import com.stripe.android.identity.databinding.ConsentFragmentBinding
import com.stripe.android.identity.networking.models.ClearDataParam
import com.stripe.android.identity.networking.models.CollectedDataParam
import com.stripe.android.identity.networking.models.VerificationPage.Companion.isMissingBiometricConsent
import com.stripe.android.identity.networking.models.VerificationPage.Companion.isUnsupportedClient
import com.stripe.android.identity.networking.models.VerificationPageStaticContentConsentPage
import com.stripe.android.identity.utils.navigateToErrorFragmentWithFailedReason
import com.stripe.android.identity.utils.postVerificationPageDataAndMaybeSubmit
import com.stripe.android.identity.utils.setHtmlString
import com.stripe.android.identity.viewmodel.ConsentFragmentViewModel
import com.stripe.android.identity.viewmodel.IdentityViewModel
import kotlinx.coroutines.launch

/**
 * The start screen of Identification flow, prompt for client's consent.
 *
 */
internal class ConsentFragment(
    private val identityViewModelFactory: ViewModelProvider.Factory,
    private val consentViewModelFactory: ViewModelProvider.Factory,
    private val fallbackUrlLauncher: FallbackUrlLauncher
) : Fragment() {
    private lateinit var binding: ConsentFragmentBinding

    private val identityViewModel: IdentityViewModel by activityViewModels {
        identityViewModelFactory
    }

    private val consentViewModel: ConsentFragmentViewModel by viewModels {
        consentViewModelFactory
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = ConsentFragmentBinding.inflate(inflater, container, false)

        consentViewModel.loadUriIntoImageView(
            identityViewModel.verificationArgs.brandLogo,
            binding.merchantLogo
        )

        binding.agree.setOnClickListener {
            binding.agree.toggleToLoading()
            binding.decline.isEnabled = false
            postVerificationPageDataAndNavigate(
                CollectedDataParam(
                    biometricConsent = true
                )
            )
        }
        binding.decline.setOnClickListener {
            binding.decline.toggleToLoading()
            binding.agree.isEnabled = false
            postVerificationPageDataAndNavigate(
                CollectedDataParam(
                    biometricConsent = false
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
                if (verificationPage.isUnsupportedClient()) {
                    Log.e(TAG, "Unsupported client, launching fallback url")
                    fallbackUrlLauncher.launchFallbackUrl(verificationPage.fallbackUrl)
                } else if (verificationPage.isMissingBiometricConsent()) {
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
                fromFragment = R.id.consentFragment,
                notSubmitBlock = {
                    navigateToDocSelection()
                }
            )
        }
    }

    private fun navigateToDocSelection() {
        findNavController().navigate(R.id.action_consentFragment_to_docSelectionFragment)
    }

    private fun bindViewData(consentPage: VerificationPageStaticContentConsentPage) {
        binding.titleText.text = consentPage.title

        consentPage.privacyPolicy?.let {
            binding.privacyPolicy.setHtmlString(it)
        } ?: run {
            binding.privacyPolicy.visibility = View.GONE
        }

        consentPage.timeEstimate?.let {
            binding.timeEstimate.text = it
        } ?: run {
            binding.timeEstimate.visibility = View.GONE
        }

        if (binding.privacyPolicy.visibility == View.GONE &&
            binding.timeEstimate.visibility == View.GONE
        ) {
            binding.divider.visibility = View.GONE
        }

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
