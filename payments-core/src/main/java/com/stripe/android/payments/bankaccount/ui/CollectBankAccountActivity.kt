package com.stripe.android.payments.bankaccount.ui

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.stripe.android.payments.bankaccount.navigation.CollectBankAccountContract
import com.stripe.android.payments.bankaccount.ui.CollectBankAccountViewEffect.OpenConnectionsFlow
import com.stripe.android.payments.connections.ConnectionsPaymentsProxy

/**
 * No-UI activity that will handle collect bank account logic.
 */
internal class CollectBankAccountActivity : AppCompatActivity() {

    private val starterArgs: CollectBankAccountContract.Args? by lazy {
        CollectBankAccountContract.Args.fromIntent(intent)
    }

    private lateinit var connectionsPaymentsProxy: ConnectionsPaymentsProxy

    internal var viewModelFactory: ViewModelProvider.Factory =
        CollectBankAccountViewModel.Factory(
            { application },
            { requireNotNull(starterArgs) },
            this,
            intent?.extras
        )

    private val viewModel: CollectBankAccountViewModel by viewModels { viewModelFactory }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initConnectionsPaymentsProxy()
        lifecycleScope.launchWhenStarted {
            viewModel.viewEffect.collect { viewEffect ->
                when (viewEffect) {
                    is OpenConnectionsFlow -> viewEffect.launch()
                    is CollectBankAccountViewEffect.FinishWithResult -> viewEffect.launch()
                }
            }
        }
    }

    private fun initConnectionsPaymentsProxy() {
        connectionsPaymentsProxy = ConnectionsPaymentsProxy.create(
            activity = this,
            onComplete = viewModel::onConnectionsResult
        )
    }

    private fun OpenConnectionsFlow.launch() {
        connectionsPaymentsProxy.present(
            linkedAccountSessionClientSecret,
            publishableKey
        )
    }

    private fun CollectBankAccountViewEffect.FinishWithResult.launch() {
        setResult(
            Activity.RESULT_OK,
            Intent().putExtras(
                CollectBankAccountContract.Result(result).toBundle()
            )
        )
        finish()
    }
}
