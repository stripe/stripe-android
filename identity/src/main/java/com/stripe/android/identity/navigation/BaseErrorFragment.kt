package com.stripe.android.identity.navigation

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.stripe.android.identity.analytics.IdentityAnalyticsRequestFactory.Companion.SCREEN_NAME_ERROR
import com.stripe.android.identity.viewmodel.IdentityViewModel
import kotlinx.coroutines.launch

/**
 * Base error fragment displaying error messages and two buttons
 */
internal abstract class BaseErrorFragment(
    private val identityViewModelFactory: ViewModelProvider.Factory
) : Fragment() {
    protected val identityViewModel: IdentityViewModel by activityViewModels {
        identityViewModelFactory
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        lifecycleScope.launch(identityViewModel.workContext) {
            identityViewModel.screenTracker.screenTransitionFinish(SCREEN_NAME_ERROR)
        }
        identityViewModel.sendAnalyticsRequest(
            identityViewModel.identityAnalyticsRequestFactory.screenPresented(
                screenName = SCREEN_NAME_ERROR
            )
        )
    }
}
