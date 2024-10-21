package com.stripe.android.payments.bankaccount.ui

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.stripe.android.payments.bankaccount.CollectBankAccountConfiguration
import com.stripe.android.payments.bankaccount.CollectBankAccountConfiguration.InstantDebits
import com.stripe.android.payments.bankaccount.CollectBankAccountConfiguration.USBankAccount
import com.stripe.android.payments.bankaccount.CollectBankAccountConfiguration.USBankAccountInternal
import com.stripe.android.payments.bankaccount.navigation.CollectBankAccountContract
import com.stripe.android.payments.bankaccount.navigation.CollectBankAccountResultInternal.Failed
import com.stripe.android.payments.bankaccount.ui.CollectBankAccountViewEffect.FinishWithResult
import com.stripe.android.payments.bankaccount.ui.CollectBankAccountViewEffect.OpenConnectionsFlow
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
        if (starterArgs?.configuration == null) {
            val failure = Failed(IllegalStateException("Configuration not provided"))
            FinishWithResult(failure).launch()
        } else {
            initConnectionsPaymentsProxy(requireNotNull(starterArgs).configuration)
            lifecycleScope.launchWhenStarted {
                viewModel.viewEffect.collect { viewEffect ->
                    when (viewEffect) {
                        is OpenConnectionsFlow -> viewEffect.launch()
                        is FinishWithResult -> viewEffect.launch()
                    }
                }
            }
        }
    }

    private fun initConnectionsPaymentsProxy(configuration: CollectBankAccountConfiguration) {
        financialConnectionsPaymentsProxy = when (configuration) {
            is InstantDebits -> FinancialConnectionsPaymentsProxy.createForInstantDebits(
                activity = this,
                onComplete = viewModel::onConnectionsForInstantDebitsResult
            )

            is USBankAccount -> FinancialConnectionsPaymentsProxy.createForACH(
                activity = this,
                onComplete = viewModel::onConnectionsForACHResult
            )

            is USBankAccountInternal -> FinancialConnectionsPaymentsProxy.createForACH(
                activity = this,
                onComplete = viewModel::onConnectionsForACHResult
            )
        }
    }

    private fun OpenConnectionsFlow.launch() {
        financialConnectionsPaymentsProxy.present(
            financialConnectionsSessionClientSecret = financialConnectionsSessionSecret,
            publishableKey = publishableKey,
            stripeAccountId = stripeAccountId,
            elementsSessionContext = elementsSessionContext,
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
