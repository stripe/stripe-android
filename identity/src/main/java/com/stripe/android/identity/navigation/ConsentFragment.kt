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
import com.stripe.android.identity.FallbackUrlLauncher
import com.stripe.android.identity.ui.ConsentScreen
import com.stripe.android.identity.viewmodel.IdentityViewModel

/**
 * The start screen of Identification flow, prompt for client's consent.
 *
 */
internal class ConsentFragment(
    private val identityViewModelFactory: ViewModelProvider.Factory,
    private val fallbackUrlLauncher: FallbackUrlLauncher
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
            ConsentScreen(
                navController = findNavController(),
                identityViewModel = identityViewModel,
                fallbackUrlLauncher = fallbackUrlLauncher
            )
        }
    }
}
