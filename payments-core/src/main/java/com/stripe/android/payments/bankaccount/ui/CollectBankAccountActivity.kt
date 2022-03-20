package com.stripe.android.payments.bankaccount.ui

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.stripe.android.payments.bankaccount.navigation.CollectBankAccountContract
import com.stripe.android.payments.bankaccount.navigation.CollectBankAccountResult
import com.stripe.android.payments.bankaccount.navigation.CollectBankAccountResult.Completed
import com.stripe.android.payments.bankaccount.navigation.CollectBankAccountResult.Failed
import com.stripe.android.payments.bankaccount.ui.CollectBankAccountViewEffect.FinishWithError
import com.stripe.android.payments.bankaccount.ui.CollectBankAccountViewEffect.FinishWithPaymentIntent
import com.stripe.android.payments.bankaccount.ui.CollectBankAccountViewEffect.FinishWithSetupIntent
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
                    is FinishWithError -> finishWithResult(Failed(Exception(viewEffect.exception)))
                    is FinishWithPaymentIntent -> finishWithResult(Completed(viewEffect.paymentIntent))
                    is FinishWithSetupIntent -> finishWithResult(Completed(viewEffect.setupIntent))
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

    private fun finishWithResult(result: CollectBankAccountResult) {
        setResult(
            Activity.RESULT_OK,
            Intent().putExtras(
                CollectBankAccountContract.Result(result).toBundle()
            )
        )
        finish()
    }
}
