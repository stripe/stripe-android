package com.stripe.android.identity.navigation

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.stripe.android.identity.VerificationFlowFinishable
import com.stripe.android.identity.ui.ConfirmationScreen
import com.stripe.android.identity.viewmodel.IdentityViewModel

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
            ConfirmationScreen(
                navController = findNavController(),
                identityViewModel = identityViewModel,
                verificationFlowFinishable = verificationFlowFinishable
            )
        }
    }
}
