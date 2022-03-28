package com.stripe.android.payments.bankaccount.ui

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.stripe.android.payments.bankaccount.navigation.CollectBankAccountContract
import com.stripe.android.payments.bankaccount.ui.CollectBankAccountViewEffect.FinishWithResult
import com.stripe.android.payments.bankaccount.ui.CollectBankAccountViewEffect.OpenConnectionsFlow

/**
 * No-UI activity that will handle collect bank account logic.
 */
internal class CollectBankAccountActivity : AppCompatActivity() {

    private val starterArgs: CollectBankAccountContract.Args? by lazy {
        CollectBankAccountContract.Args.fromIntent(intent)
    }

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
        lifecycleScope.launchWhenStarted {
            viewModel.viewEffect.collect { viewEffect ->
                when (viewEffect) {
                    is OpenConnectionsFlow -> viewEffect.launch()
                    is FinishWithResult -> viewEffect.launch()
                }
            }
        }
    }

    private fun OpenConnectionsFlow.launch() {
        // TODO@carlosmuvi open connections flow and wait for result instead.
        viewModel.onConnectionsResult("resulting_linked_account_session")
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
