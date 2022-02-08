package com.stripe.android.identity.navigation

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.stripe.android.identity.R
import com.stripe.android.identity.databinding.ConsentFragmentBinding

/**
 * The start screen of Identification flow, prompt for client's consent.
 *
 */
internal class ConsentFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val binding = ConsentFragmentBinding.inflate(inflater, container, false)
        val args = requireNotNull(arguments)

        binding.merchantLogo.setImageResource(args[ARG_MERCHANT_LOGO] as Int)

        // TODO(ccen) set the text of other field, decide if we should get separate values from different fields or a single value as an html string
        binding.howContent.text = args[ARG_CONSENT_CONTEXT] as String

        binding.agree.setOnClickListener {
            findNavController().navigate(R.id.action_consentFragment_to_docSelectionFragment)
        }
        binding.decline.setOnClickListener {
            Log.d(TAG, "declined")
        }
        return binding.root
    }

    internal companion object {
        val TAG: String = ConsentFragment::class.java.simpleName
        const val ARG_CONSENT_CONTEXT = "consentContext"
        const val ARG_MERCHANT_LOGO = "merchantLogo"
    }
}
