package com.stripe.android.financialconnections

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.OnBackPressedCallback
import androidx.activity.addCallback
import androidx.fragment.app.Fragment
import androidx.activity.result.contract.ActivityResultContracts
import androidx.viewbinding.ViewBinding
import com.airbnb.mvrx.MavericksView
import com.airbnb.mvrx.UniqueOnly
import com.airbnb.mvrx.activityViewModel
import com.stripe.android.financialconnections.FinancialConnectionsSheetViewEffect.FinishWithResult
import com.stripe.android.financialconnections.FinancialConnectionsSheetViewEffect.OpenAuthFlowWithUrl
import com.stripe.android.financialconnections.databinding.FragmentFinancialConnectionsSheetBinding
import com.stripe.android.financialconnections.launcher.FinancialConnectionsSheetActivityResult
import com.stripe.android.financialconnections.presentation.CreateBrowserIntentForUrl

class FinancialConnectionsSheetFragment : Fragment(), MavericksView {

    private val startForResult =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            viewModel.onActivityResult()
        }

    private val viewBinding: FragmentFinancialConnectionsSheetBinding by viewBinding()
    private val viewModel: FinancialConnectionsSheetViewModel by activityViewModel()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        //TODO validate args!
        observeAsyncs()
        if (savedInstanceState != null) viewModel.onActivityRecreated()
    }

    private fun observeAsyncs() {
        viewModel.onAsync(
            asyncProp = FinancialConnectionsSheetState::sideEffect,
            deliveryMode = UniqueOnly(subscriptionId = "financial-connections-view-effect"),
            onSuccess = { viewEffect ->
                when (viewEffect) {
                    is OpenAuthFlowWithUrl -> viewEffect.launch()
                    is FinishWithResult -> finishWithResult(viewEffect.result)
                }
            },
        )
    }

    override fun invalidate() = Unit

    private fun OpenAuthFlowWithUrl.launch() {
        val uri = Uri.parse(this.url)
        startForResult.launch(
            CreateBrowserIntentForUrl(
                context = requireContext(),
                uri = uri,
            )
        )
    }

    override fun onResume() {
        super.onResume()
        viewModel.onResume()
    }

    private fun finishWithResult(result: FinancialConnectionsSheetActivityResult) {
        requireActivity().setResult(
            Activity.RESULT_OK,
            Intent().putExtras(result.toBundle())
        )
        requireActivity().finish()
    }
}


/**
 * Create bindings for a view similar to bindView.
 *
 * To use, just call
 * private val binding: FHomeWorkoutDetailsBinding by viewBinding()
 * with your binding class and access it as you normally would.
 */
inline fun <reified T : ViewBinding> Fragment.viewBinding() =
    FragmentViewBindingDelegate(T::class.java, this)