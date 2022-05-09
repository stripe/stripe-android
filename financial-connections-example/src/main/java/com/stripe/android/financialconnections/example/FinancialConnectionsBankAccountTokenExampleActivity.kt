package com.stripe.android.financialconnections.example

import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.stripe.android.financialconnections.example.databinding.ActivityFinancialconnectionsExampleBinding
import com.stripe.android.financialconnections.FinancialConnectionsSheet
import com.stripe.android.financialconnections.example.FinancialConnectionsExampleViewEffect.OpenFinancialConnectionsSheetExample

class FinancialConnectionsBankAccountTokenExampleActivity : AppCompatActivity() {

    private val viewModel by viewModels<FinancialConnectionsExampleViewModel>()

    private lateinit var financialConnectionsSheet: FinancialConnectionsSheet

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        with(ActivityFinancialconnectionsExampleBinding.inflate(layoutInflater)) {
            setContentView(root)
            setupViews()
            observeViews()
            observeState()
        }
    }

    private fun ActivityFinancialconnectionsExampleBinding.setupViews() {
        toolbar.setTitle(R.string.collect_bank_account_for_bank_account_token_title)
        setSupportActionBar(toolbar)
        //TODO@carlosmuvi uncomment for pay-outs.
//        financialConnectionsSheet = FinancialConnectionsSheet.createForBankAccountToken(
//            activity = this@FinancialConnectionsBankAccountTokenExampleActivity,
//            callback = viewModel::onFinancialConnectionsSheetForBankAccountTokenResult
//        )
    }

    private fun ActivityFinancialconnectionsExampleBinding.observeViews() {
        launchConnectionsSheet.setOnClickListener { viewModel.startFinancialConnectionsSessionForToken() }
    }

    private fun ActivityFinancialconnectionsExampleBinding.observeState() {
        lifecycleScope.launchWhenStarted {
            viewModel.state.collect {
                bindState(it, this@observeState)
            }
        }
        lifecycleScope.launchWhenStarted {
            viewModel.viewEffect.collect {
                when (it) {
                    is OpenFinancialConnectionsSheetExample -> financialConnectionsSheet.present(it.configuration)
                }
            }
        }
    }

    private fun bindState(
        financialConnectionsExampleState: FinancialConnectionsExampleState,
        viewBinding: ActivityFinancialconnectionsExampleBinding
    ) {
        viewBinding.status.text = financialConnectionsExampleState.status
        viewBinding.launchConnectionsSheet.isEnabled =
            financialConnectionsExampleState.loading.not()
    }
}
