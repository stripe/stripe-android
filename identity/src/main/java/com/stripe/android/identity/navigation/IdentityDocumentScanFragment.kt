package com.stripe.android.identity.navigation

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.annotation.StringRes
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.stripe.android.identity.networking.models.CollectedDataParam
import com.stripe.android.identity.states.IdentityScanState
import com.stripe.android.identity.ui.DocumentScanMessageRes
import com.stripe.android.identity.ui.DocumentScanScreen
import com.stripe.android.identity.viewmodel.IdentityScanViewModel
import com.stripe.android.identity.viewmodel.IdentityViewModel

/**
 * Fragment for scanning ID, Passport and Driver's license
 */
internal abstract class IdentityDocumentScanFragment(
    identityCameraScanViewModelFactory: ViewModelProvider.Factory,
    identityViewModelFactory: ViewModelProvider.Factory
) : Fragment() {
    val identityScanViewModel: IdentityScanViewModel by viewModels { identityCameraScanViewModelFactory }
    val identityViewModel: IdentityViewModel by activityViewModels { identityViewModelFactory }

    protected abstract val destinationRoute: IdentityTopLevelDestination.DestinationRoute

    abstract val frontScanType: IdentityScanState.ScanType
    abstract val backScanType: IdentityScanState.ScanType?

    @get:StringRes
    abstract val frontTitleStringRes: Int

    @get:StringRes
    abstract val backTitleStringRes: Int

    @get:StringRes
    abstract val frontMessageStringRes: Int

    @get:StringRes
    abstract val backMessageStringRes: Int

    abstract val collectedDataParamType: CollectedDataParam.Type

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ) = ComposeView(requireContext()).apply {
        setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
        setContent {
            val changedDisplayState by identityScanViewModel.displayStateChangedFlow.collectAsState()
            val newDisplayState by remember {
                derivedStateOf {
                    changedDisplayState?.first
                }
            }

            DocumentScanScreen(
                navController = findNavController(),
                identityViewModel = identityViewModel,
                identityScanViewModel = identityScanViewModel,
                frontScanType = frontScanType,
                backScanType = backScanType,
                shouldStartFromBack = shouldStartFromBack(),
                messageRes = DocumentScanMessageRes(
                    frontTitleStringRes,
                    backTitleStringRes,
                    frontMessageStringRes,
                    backMessageStringRes
                ),
                newDisplayState = newDisplayState,
                collectedDataParamType = collectedDataParamType,
                route = destinationRoute.route
            )
        }
    }

    /**
     * Check if should start scanning from back.
     */
    private fun shouldStartFromBack(): Boolean =
        arguments?.getBoolean(ARG_SHOULD_START_FROM_BACK) == true

    internal companion object {
        const val ARG_SHOULD_START_FROM_BACK = "startFromBack"
    }
}
