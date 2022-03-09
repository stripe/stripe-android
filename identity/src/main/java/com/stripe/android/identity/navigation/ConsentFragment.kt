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
import com.stripe.android.identity.networking.Status
import com.stripe.android.identity.networking.models.CollectedDataParam
import com.stripe.android.identity.networking.models.ConsentParam
import com.stripe.android.identity.networking.models.VerificationPage.Companion.isMissingBiometricConsent
import com.stripe.android.identity.networking.models.VerificationPageData.Companion.isMissingDocumentType
import com.stripe.android.identity.networking.models.VerificationPageStaticContentConsentPage
import com.stripe.android.identity.utils.navigateToDefaultErrorFragment
import com.stripe.android.identity.utils.postVerificationPageDataAndMaybeSubmit
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
            postVerificationPageDataAndNavigate(
                CollectedDataParam(
                    consent = ConsentParam(biometric = true)
                )
            )
        }
        binding.decline.setOnClickListener {
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
        identityViewModel.verificationPage.observe(viewLifecycleOwner) {
            when (it.status) {
                Status.SUCCESS -> {
                    if (requireNotNull(it.data).isMissingBiometricConsent()) {
                        setLoadingFinishedUI()
                        bindViewData(requireNotNull(it.data).biometricConsent)
                    } else {
                        navigateToDocSelection()
                    }
                }
                Status.LOADING -> {} // no-op
                Status.ERROR -> {
                    Log.d(
                        TAG,
                        "API Error occurred: $it.throwable, navigate to general error fragment"
                    )
                    navigateToDefaultErrorFragment()
                }
            }
        }
    }

    /**
     * Post VerificationPageData with the type and navigate base on its result.
     */
    private fun postVerificationPageDataAndNavigate(collectedDataParam: CollectedDataParam) {
        lifecycleScope.launch {
            postVerificationPageDataAndMaybeSubmit(
                identityViewModel,
                collectedDataParam,
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
        binding.agree.text = consentPage.acceptButtonText
        binding.decline.text = consentPage.declineButtonText
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
