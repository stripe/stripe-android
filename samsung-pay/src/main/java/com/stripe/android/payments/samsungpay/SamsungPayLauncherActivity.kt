package com.stripe.android.payments.samsungpay

import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.os.bundleOf
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import kotlin.getValue

internal class SamsungPayLauncherActivity : AppCompatActivity() {

    private val viewModel: SamsungPayViewModel by viewModels {
        SamsungPayViewModel.Factory()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val args = runCatching {
            requireNotNull(SamsungPayLauncherContract.Args.fromIntent(intent)) {
                "SamsungPayLauncherActivity was started without arguments."
            }
        }.getOrElse {
            finishWithResult(SamsungPayResult.Failure(it))
            return
        }

        lifecycleScope.launch {
            viewModel.samsungPayResult.collect { result ->
                finishWithResult(result)
            }
        }

        viewModel.startPayment(this)
    }

    private fun finishWithResult(result: SamsungPayResult) {
        setResult(
            RESULT_OK,
            Intent().putExtras(
                bundleOf(SamsungPayLauncherContract.EXTRA_RESULT to result)
            )
        )
        finish()
    }
}
