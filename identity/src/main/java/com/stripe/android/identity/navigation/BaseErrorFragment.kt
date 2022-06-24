package com.stripe.android.identity.navigation

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.google.android.material.button.MaterialButton
import com.stripe.android.identity.analytics.IdentityAnalyticsRequestFactory.Companion.SCREEN_NAME_ERROR
import com.stripe.android.identity.databinding.BaseErrorFragmentBinding
import com.stripe.android.identity.viewmodel.IdentityViewModel
import kotlinx.coroutines.Dispatchers
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

    protected lateinit var title: TextView
    protected lateinit var message1: TextView
    protected lateinit var message2: TextView
    protected lateinit var topButton: MaterialButton
    protected lateinit var bottomButton: MaterialButton

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val binding = BaseErrorFragmentBinding.inflate(inflater, container, false)
        title = binding.titleText
        message1 = binding.message1
        message2 = binding.message2
        topButton = binding.topButton
        bottomButton = binding.bottomButton
        onCustomizingViews()
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        lifecycleScope.launch(Dispatchers.IO) {
            identityViewModel.screenTracker.screenTransitionFinish(SCREEN_NAME_ERROR)
        }
        identityViewModel.sendAnalyticsRequest(
            identityViewModel.identityAnalyticsRequestFactory.screenPresented(
                screenName = SCREEN_NAME_ERROR
            )
        )
    }

    protected abstract fun onCustomizingViews()
}
