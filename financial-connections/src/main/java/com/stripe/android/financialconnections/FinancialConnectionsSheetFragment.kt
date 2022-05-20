package com.stripe.android.financialconnections

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.addCallback
import androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult
import androidx.fragment.app.Fragment
import com.airbnb.mvrx.MavericksView
import com.airbnb.mvrx.activityViewModel
import com.airbnb.mvrx.withState
import com.stripe.android.financialconnections.FinancialConnectionsSheetViewEffect.FinishWithResult
import com.stripe.android.financialconnections.FinancialConnectionsSheetViewEffect.OpenAuthFlowWithUrl
import com.stripe.android.financialconnections.launcher.FinancialConnectionsSheetActivityResult
import com.stripe.android.financialconnections.presentation.CreateBrowserIntentForUrl

internal class FinancialConnectionsSheetFragment :
    Fragment(R.layout.fragment_financial_connections_sheet), MavericksView {

    private val startForResult = registerForActivityResult(StartActivityForResult()) {
        viewModel.onActivityResult()
    }

    private val viewModel: FinancialConnectionsSheetViewModel by activityViewModel()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requireActivity().onBackPressedDispatcher.addCallback(this) {
            finishWithResult(FinancialConnectionsSheetActivityResult.Canceled)
        }
    }

    /**
     * handle state changes here.
     */
    override fun invalidate() {
        withState(viewModel) { state ->
            state.viewEffect?.let {
                when (it) {
                    is OpenAuthFlowWithUrl -> startForResult.launch(
                        CreateBrowserIntentForUrl(
                            context = requireContext(),
                            uri = Uri.parse(it.url),
                        )
                    )
                    is FinishWithResult -> finishWithResult(it.result)
                }
                viewModel.onViewEffectLaunched()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.onResume()
    }

    private fun finishWithResult(result: FinancialConnectionsSheetActivityResult) {
        with(requireActivity()) {
            setResult(Activity.RESULT_OK, Intent().putExtras(result.toBundle()))
            finish()
        }
    }
}
