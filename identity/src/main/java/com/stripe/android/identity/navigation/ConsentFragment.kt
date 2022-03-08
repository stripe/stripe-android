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
import androidx.navigation.fragment.findNavController
import com.stripe.android.identity.R
import com.stripe.android.identity.databinding.ConsentFragmentBinding
import com.stripe.android.identity.navigation.ErrorFragment.Companion.navigateToErrorFragmentWithDefaultValues
import com.stripe.android.identity.navigation.ErrorFragment.Companion.navigateToErrorFragmentWithRequirementErrorAndDestination
import com.stripe.android.identity.networking.Status
import com.stripe.android.identity.networking.models.CollectedDataParam
import com.stripe.android.identity.networking.models.ConsentParam
import com.stripe.android.identity.networking.models.VerificationPageData
import com.stripe.android.identity.networking.models.VerificationPageStaticContentConsentPage
import com.stripe.android.identity.utils.setHtmlString
import com.stripe.android.identity.viewmodel.IdentityViewModel
import kotlinx.coroutines.launch

/**
 * The start screen of Identification flow, prompt for client's consent.
 *
 */
internal class ConsentFragment(
    private val identityViewModelFactory: ViewModelProvider.Factory
) : Fragment() {
    private lateinit var binding: ConsentFragmentBinding

    private val identityViewModel: IdentityViewModel by activityViewModels {
        identityViewModelFactory
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = ConsentFragmentBinding.inflate(inflater, container, false)

        binding.merchantLogo.setImageResource(identityViewModel.args.merchantLogo)

        binding.agree.setOnClickListener {
            postVerificationPageData(
                CollectedDataParam(
                    consent = ConsentParam(biometric = true)
                )
            )
        }
        binding.decline.setOnClickListener {
            postVerificationPageData(
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
        identityViewModel.verificationPage.observe(viewLifecycleOwner) {
            when (it.status) {
                Status.SUCCESS -> {
                    setLoadingFinishedUI()
                    bindViewData(requireNotNull(it.data).biometricConsent)
                }
                Status.LOADING -> {
                    // no-op
                }
                Status.ERROR -> {
                    navigateOnApiError(requireNotNull(it.throwable))
                }
            }
        }
    }

    private fun postVerificationPageData(collectedDataParam: CollectedDataParam) {
        lifecycleScope.launch {
            runCatching {
                identityViewModel.postVerificationPageData(
                    collectedDataParam
                )
            }.fold(
                onSuccess = ::navigateOnVerificationPageData,
                onFailure = ::navigateOnApiError
            )
        }
    }

    private fun bindViewData(consentPage: VerificationPageStaticContentConsentPage) {
        binding.titleText.text = consentPage.title
        binding.privacyPolicy.setHtmlString(consentPage.privacyPolicy)
        binding.timeEstimate.text = consentPage.timeEstimate
        binding.body.setHtmlString(consentPage.body)
        binding.agree.text = consentPage.acceptButtonText
        binding.decline.text = consentPage.declineButtonText
    }

    private fun setLoadingFinishedUI() {
        binding.loadings.visibility = View.GONE
        binding.texts.visibility = View.VISIBLE
        binding.buttons.visibility = View.VISIBLE
    }

    private fun navigateOnVerificationPageData(verificationPageData: VerificationPageData) {
        if (verificationPageData.requirements.errors.isEmpty()) {
            findNavController().navigate(R.id.action_consentFragment_to_docSelectionFragment)
        } else {
            findNavController().navigateToErrorFragmentWithRequirementErrorAndDestination(
                verificationPageData.requirements.errors[0],
                R.id.action_errorFragment_to_consentFragment
            )
        }
    }

    private fun navigateOnApiError(throwable: Throwable) {
        Log.d(TAG, "API Error occurred: $throwable, navigate to general error fragment")
        findNavController().navigateToErrorFragmentWithDefaultValues(requireContext())
    }

    private companion object {
        val TAG: String = ConsentFragment::class.java.simpleName
    }
}
