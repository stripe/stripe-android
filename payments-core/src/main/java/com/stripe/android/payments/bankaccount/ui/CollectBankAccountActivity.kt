package com.stripe.android.payments.bankaccount.ui

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.stripe.android.financialconnections.FinancialConnectionsSheet
import com.stripe.android.payments.bankaccount.CollectBankAccountConfiguration.InstantDebits
import com.stripe.android.payments.bankaccount.CollectBankAccountConfiguration.USBankAccount
import com.stripe.android.payments.bankaccount.navigation.CollectBankAccountContract
import com.stripe.android.payments.bankaccount.ui.CollectBankAccountViewEffect.FinishWithResult
import com.stripe.android.payments.bankaccount.ui.CollectBankAccountViewEffect.OpenConnectionsFlow
import com.stripe.android.payments.financialconnections.DefaultFinancialConnectionsPaymentsProxy
import com.stripe.android.payments.financialconnections.FinancialConnectionsPaymentsProxy

/**
 * No-UI activity that will handle collect bank account logic.
 */
internal class CollectBankAccountActivity : AppCompatActivity() {

    private val starterArgs: CollectBankAccountContract.Args? by lazy {
        CollectBankAccountContract.Args.fromIntent(intent)
    }

    private lateinit var financialConnectionsPaymentsProxy: FinancialConnectionsPaymentsProxy

    private val viewModel: CollectBankAccountViewModel by viewModels {
        CollectBankAccountViewModel.Factory {
            requireNotNull(starterArgs)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        starterArgs?.let {
            initConnectionsPaymentsProxy(it)
        }

        lifecycleScope.launchWhenStarted {
            viewModel.viewEffect.collect { viewEffect ->
                when (viewEffect) {
                    is OpenConnectionsFlow -> viewEffect.launch()
                    is FinishWithResult -> viewEffect.launch()
                }
            }
        }
    }

    private fun initConnectionsPaymentsProxy(args: CollectBankAccountContract.Args) {
        financialConnectionsPaymentsProxy = when (args.configuration) {
            is InstantDebits -> {
                FinancialConnectionsPaymentsProxy.create(
                    activity = this,
                    onComplete = viewModel::onConnectionsResult,
                    provider = {
                        DefaultFinancialConnectionsPaymentsProxy(
                            FinancialConnectionsSheet.createForLink(
                                activity = this,
                                callback = { /* TODO Coming soon… */ },
                            )
                        )
                    },
                )
            }
            is USBankAccount -> {
                FinancialConnectionsPaymentsProxy.create(
                    activity = this,
                    onComplete = viewModel::onConnectionsResult
                )
            }
        }
    }

    private fun OpenConnectionsFlow.launch() {
        financialConnectionsPaymentsProxy.present(
            financialConnectionsSessionClientSecret = financialConnectionsSessionSecret,
            publishableKey = publishableKey,
            stripeAccountId = stripeAccountId
        )
    }

    private fun FinishWithResult.launch() {
        setResult(
            Activity.RESULT_OK,
            Intent().putExtras(
                CollectBankAccountContract.Result(result).toBundle()
            )
        )
        finish()
    }
}
