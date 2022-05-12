package com.stripe.android.financialconnections

import android.app.Activity
import android.content.Intent
import android.net.Uri
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

    /**
     * handle state changes here.
     */
    override fun invalidate() {
        withState(viewModel) { state ->
            if (state.viewEffect != null) {
                when (state.viewEffect) {
                    is OpenAuthFlowWithUrl -> state.viewEffect.launch()
                    is FinishWithResult -> finishWithResult(state.viewEffect.result)
                }
                viewModel.onViewEffectLaunched()
            }
        }
    }

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
