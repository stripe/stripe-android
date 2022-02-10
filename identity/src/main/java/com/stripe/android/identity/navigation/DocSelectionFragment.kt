package com.stripe.android.identity.navigation

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.stripe.android.identity.R
import com.stripe.android.identity.databinding.DocSelectionFragmentBinding

/**
 * Screen to select type of ID to scan.
 */
internal class DocSelectionFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val binding = DocSelectionFragmentBinding.inflate(inflater, container, false)
        binding.dl.setOnClickListener {
            findNavController().navigate(R.id.action_docSelectionFragment_to_driverLicenseScanFragment)
        }

        binding.id.setOnClickListener {
            findNavController().navigate(R.id.action_docSelectionFragment_to_IDScanFragment)
        }

        binding.passport.setOnClickListener {
            findNavController().navigate(R.id.action_docSelectionFragment_to_passportScanFragment)
        }

        return binding.root
    }
}
