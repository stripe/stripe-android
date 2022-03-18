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
            viewModel.viewEffect.collect {
                when (it) {
                    is OpenConnectionsFlow -> {
                        // TODO launch connections flow and wait for result.
                        viewModel.onConnectionsResult("account_session_id")
                    }
                    is FinishWithError -> finishWithResult(Failed(Exception(it.exception)))
                    is FinishWithPaymentIntent -> finishWithResult(Completed(it.paymentIntent))
                    is FinishWithSetupIntent -> finishWithResult(Completed(it.setupIntent))
                }
            }
        }
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
