package com.stripe.android.payments.bankaccount.ui

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.stripe.android.payments.bankaccount.navigation.CollectSessionForDeferredPaymentsContract
import com.stripe.android.payments.financialconnections.FinancialConnectionsPaymentsProxy

/**
 * No-UI activity that will handle collect session logic.
 */
internal class CollectSessionForDeferredPaymentsActivity : AppCompatActivity() {

    private val starterArgs: CollectSessionForDeferredPaymentsContract.Args? by lazy {
        CollectSessionForDeferredPaymentsContract.Args.fromIntent(intent)
    }

    private lateinit var financialConnectionsPaymentsProxy: FinancialConnectionsPaymentsProxy

    private val viewModel: CollectSessionForDeferredPaymentsViewModel by viewModels {
        CollectSessionForDeferredPaymentsViewModel.Factory {
            requireNotNull(starterArgs)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initConnectionsPaymentsProxy()
        lifecycleScope.launchWhenStarted {
            viewModel.viewEffect.collect { viewEffect ->
                when (viewEffect) {
                    is CollectSessionForDeferredPaymentsViewEffect.OpenConnectionsFlow -> viewEffect.launch()
                    is CollectSessionForDeferredPaymentsViewEffect.FinishWithResult -> viewEffect.launch()
                }
            }
        }
    }

    private fun initConnectionsPaymentsProxy() {
        financialConnectionsPaymentsProxy = FinancialConnectionsPaymentsProxy.create(
            activity = this,
            onComplete = viewModel::onConnectionsResult
        )
    }

    private fun CollectSessionForDeferredPaymentsViewEffect.OpenConnectionsFlow.launch() {
        financialConnectionsPaymentsProxy.present(
            financialConnectionsSessionClientSecret = financialConnectionsSessionSecret,
            publishableKey = publishableKey,
            stripeAccountId = stripeAccountId
        )
    }

    private fun CollectSessionForDeferredPaymentsViewEffect.FinishWithResult.launch() {
        setResult(
            Activity.RESULT_OK,
            Intent().putExtras(
                CollectSessionForDeferredPaymentsContract.Result(result).toBundle()
            )
        )
        finish()
    }
}
