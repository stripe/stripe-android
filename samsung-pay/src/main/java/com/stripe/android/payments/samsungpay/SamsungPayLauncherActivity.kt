package com.stripe.android.payments.samsungpay

import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.os.bundleOf
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch

internal class SamsungPayLauncherActivity : AppCompatActivity() {

    private lateinit var args: SamsungPayLauncherContract.Args

    private val viewModel: SamsungPayLauncherViewModel by viewModels {
        SamsungPayLauncherViewModel.Factory(
            args = args,
            tokenExchangeHandler = SamsungPayLauncher.tokenExchangeHandlerHolder,
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        args = runCatching {
            requireNotNull(SamsungPayLauncherContract.Args.fromIntent(intent)) {
                "SamsungPayLauncherActivity was started without arguments."
            }
        }.getOrElse {
            finishWithResult(
                SamsungPayLauncher.Result.Failed(
                    SamsungPayException(errorCode = -1, message = it.message)
                )
            )
            return
        }

        lifecycleScope.launch {
            viewModel.result.filterNotNull().collect { result ->
                finishWithResult(result)
            }
        }

        if (savedInstanceState == null) {
            viewModel.startPayment(args)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (isFinishing) {
            SamsungPayLauncher.tokenExchangeHandlerHolder = null
        }
    }

    private fun finishWithResult(result: SamsungPayLauncher.Result) {
        setResult(
            RESULT_OK,
            Intent().putExtras(
                bundleOf(SamsungPayLauncherContract.EXTRA_RESULT to result)
            )
        )
        finish()
    }
}
