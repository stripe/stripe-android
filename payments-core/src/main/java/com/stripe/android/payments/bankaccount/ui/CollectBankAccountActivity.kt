package com.stripe.android.payments.bankaccount.ui

import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import androidx.annotation.RestrictTo
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.stripe.android.financialconnections.FinancialConnectionsSheetConfiguration
import com.stripe.android.financialconnections.launcher.FinancialConnectionsSheetLauncher
import com.stripe.android.payments.bankaccount.domain.BuildFinancialConnectionsLauncher
import com.stripe.android.payments.bankaccount.navigation.CollectBankAccountContract
import com.stripe.android.payments.bankaccount.navigation.CollectBankAccountResultInternal.Failed
import com.stripe.android.payments.bankaccount.ui.CollectBankAccountViewEffect.FinishWithResult
import com.stripe.android.payments.bankaccount.ui.CollectBankAccountViewEffect.OpenConnectionsFlow

/**
 * No-UI activity that will handle collect bank account logic.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class CollectBankAccountActivity : AppCompatActivity() {

    private val starterArgs: CollectBankAccountContract.Args? by lazy {
        CollectBankAccountContract.Args.fromIntent(intent)
    }

    private lateinit var financialConnectionsLauncher: FinancialConnectionsSheetLauncher

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
            val args = requireNotNull(starterArgs)
            val financialConnectionsAvailability = requireNotNull(args.financialConnectionsAvailability)
            financialConnectionsLauncher = BuildFinancialConnectionsLauncher(
                activity = this,
                configuration = args.configuration,
                financialConnectionsAvailability = financialConnectionsAvailability,
                onConnectionsForInstantDebitsResult = viewModel::onConnectionsForInstantDebitsResult,
                onConnectionsForACHResult = viewModel::onConnectionsForACHResult
            )
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

    private fun OpenConnectionsFlow.launch() {
        financialConnectionsLauncher.present(
            FinancialConnectionsSheetConfiguration(
                financialConnectionsSessionClientSecret = financialConnectionsSessionSecret,
                publishableKey = publishableKey,
                stripeAccountId = stripeAccountId,
            ),
            elementsSessionContext = elementsSessionContext
        )
    }

    private fun FinishWithResult.launch() {
        setResult(
            RESULT_OK,
            Intent().putExtras(
                CollectBankAccountContract.Result(result).toBundle()
            )
        )
        finish()
    }
}
