package com.stripe.android.financialconnections.example

import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.lifecycle.lifecycleScope
import com.stripe.android.financialconnections.FinancialConnectionsSheet
import com.stripe.android.financialconnections.example.FinancialConnectionsExampleViewEffect.OpenFinancialConnectionsSheetExample

class FinancialConnectionsBankAccountTokenExampleActivity : AppCompatActivity() {

    private val viewModel by viewModels<FinancialConnectionsExampleViewModel>()

    private lateinit var financialConnectionsSheet: FinancialConnectionsSheet

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_financialconnections_example)
        setupViews()
        observeViews()
        observeState()
    }

    private fun setupViews() {
        findViewById<Toolbar>(R.id.toolbar).let {
            it.title = getString(R.string.collect_bank_account_for_data_title)
            setSupportActionBar(it)
        }
        financialConnectionsSheet = FinancialConnectionsSheet.createForBankAccountToken(
            activity = this@FinancialConnectionsBankAccountTokenExampleActivity,
            callback = viewModel::onFinancialConnectionsSheetForBankAccountTokenResult
        )
    }

    private fun observeViews() {
        findViewById<View>(R.id.launch_connections_sheet)
            .setOnClickListener { viewModel.startFinancialConnectionsSessionForToken() }
    }

    private fun observeState() {
        lifecycleScope.launchWhenStarted {
            viewModel.state.collect { bindState(it) }
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
    ) {
        findViewById<TextView>(R.id.status).text =
            financialConnectionsExampleState.status
        findViewById<View>(R.id.launch_connections_sheet).isEnabled =
            financialConnectionsExampleState.loading.not()
    }
}
