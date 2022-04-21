package com.stripe.android.financialconnections.example

import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.stripe.android.financialconnections.example.databinding.ActivityFinancialconnectionsExampleBinding
import com.stripe.android.financialconnections.example.FinancialConnectionsExampleViewEffect.OpenFinancialConnectionsSheetExample
import com.stripe.android.financialconnections.FinancialConnectionsSheet
import com.stripe.android.financialconnections.example.FinancialConnectionsExampleViewEffect.OpenFinancialConnectionsSheetForTokenExample

class FinancialConnectionsExampleActivity : AppCompatActivity() {

    private val viewModel by viewModels<FinancialConnectionsExampleViewModel>()

    /**
     * Instance used when connecting bank account to retrieve data.
     */
    private lateinit var financialConnectionsSheetForData: FinancialConnectionsSheet

    /**
     * Instance used when connecting bank account to retrieve a bank account token.
     */
    private lateinit var financialConnectionsSheetForToken: FinancialConnectionsSheet

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
        setSupportActionBar(toolbar)
        financialConnectionsSheetForData = FinancialConnectionsSheet.create(
            activity = this@FinancialConnectionsExampleActivity,
            callback = viewModel::onFinancialConnectionsSheetResult
        )
        // TODO@carlosmuvi uncomment when feature exposed.
//        financialConnectionsSheetForData = FinancialConnectionsSheet.createForBankAccountToken(
//            activity = this@FinancialConnectionsExampleActivity,
//            callback = viewModel::onFinancialConnectionsSheetForBankAccountTokenResult
//        )
    }

    private fun ActivityFinancialconnectionsExampleBinding.observeViews() {
        launchConnectionsSheet.setOnClickListener { viewModel.startLinkAccountSessionForData() }
        launchConnectionsSheetForToken.setOnClickListener { viewModel.startLinkAccountSessionForToken() }
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
                    is OpenFinancialConnectionsSheetExample ->
                        financialConnectionsSheetForData.present(it.configuration)
                    is OpenFinancialConnectionsSheetForTokenExample ->
                        financialConnectionsSheetForToken.present(it.configuration)
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
