package com.stripe.android.payments.samsungpay

import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.lifecycle.lifecycleScope
import com.stripe.android.core.Logger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

class SamsungPayLauncher internal constructor(
    lifecycleScope: CoroutineScope,
    private val config: Config,
    private val readyCallback: ReadyCallback,
    private val activityResultLauncher: ActivityResultLauncher<SamsungPayLauncherContract.Args>,
    private val getSamsungPayStatus: GetSamsungPayStatus,
) {
    private var isReady = false

    constructor(
        activity: ComponentActivity,
        config: Config,
        readyCallback: ReadyCallback,
        resultCallback: ResultCallback,
    ) : this(
        lifecycleScope = activity.lifecycleScope,
        config = config,
        readyCallback = readyCallback,
        activityResultLauncher = activity.registerForActivityResult(
            SamsungPayLauncherContract()
        ) {
            resultCallback.onResult(it)
        },
        getSamsungPayStatus = DefaultGetSamsungPayStatus(
            context = activity.applicationContext,
            logger = Logger.noop()
        )
    )

    init {
        lifecycleScope.launch {
            val status = getSamsungPayStatus()
            val ready = status is GetSamsungPayStatus.Status.Ready
            isReady = ready
            readyCallback.onReady(ready)
        }
    }

    fun presentForPaymentIntent(clientSecret: String) {
        check(isReady) {
            "presentForPaymentIntent() may only be called when Samsung Pay is available on this device."
        }

        activityResultLauncher.launch(
            SamsungPayLauncherContract.Args(
                clientSecret = clientSecret,
                config = config,
            )
        )
    }

    fun interface ReadyCallback {
        fun onReady(isReady: Boolean)
    }

    fun interface ResultCallback {
        fun onResult(result: SamsungPayResult)
    }
}
