package com.stripe.android.identity.navigation

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.stripe.android.identity.databinding.ConfirmationFragmentBinding

/**
 * Fragment for confirmation.
 */
internal class ConfirmationFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val binding = ConfirmationFragmentBinding.inflate(inflater, container, false)
        // TODO(ccen) set text to bindings from network response

        return binding.root
    }
}
