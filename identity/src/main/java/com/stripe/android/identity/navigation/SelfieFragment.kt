package com.stripe.android.identity.navigation

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.findNavController
import com.stripe.android.identity.ui.SelfieScanScreen
import com.stripe.android.identity.viewmodel.IdentityScanViewModel
import com.stripe.android.identity.viewmodel.IdentityViewModel

/**
 * Fragment to capture selfie.
 */
internal class SelfieFragment(
    identityCameraScanViewModelFactory: ViewModelProvider.Factory,
    identityViewModelFactory: ViewModelProvider.Factory
) : Fragment() {
    private val identityScanViewModel: IdentityScanViewModel by viewModels { identityCameraScanViewModelFactory }
    private val identityViewModel: IdentityViewModel by activityViewModels { identityViewModelFactory }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ) = ComposeView(requireContext()).apply {
        setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
        setContent {
            SelfieScanScreen(
                navController = findNavController(),
                identityViewModel = identityViewModel,
                identityScanViewModel = identityScanViewModel
            )
        }
    }
}
