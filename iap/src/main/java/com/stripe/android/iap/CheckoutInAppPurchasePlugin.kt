package com.stripe.android.iap

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.activity.result.ActivityResultCaller
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContract
import androidx.core.os.BundleCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

internal class CheckoutInAppPurchasePlugin(
    private val createCheckoutUrlProvider: suspend (priceId: String) -> String
) : InAppPurchasePlugin() {
    private var launcher: ActivityResultLauncher<String>? = null
    private var resultCallback: InAppPurchase.ResultCallback? = null

    override fun register(
        activity: Activity,
        activityResultCaller: ActivityResultCaller,
        resultCallback: InAppPurchase.ResultCallback,
    ) {
        this.resultCallback = resultCallback
        launcher = activityResultCaller.registerForActivityResult(CheckoutContract) { result ->
            resultCallback.onResult(result)
        }
    }

    override fun unregister() {
        launcher?.unregister()
        launcher = null
        resultCallback = null
    }

    override val analyticsName: String = "checkout"

    override suspend fun purchase(priceId: String) {
        val checkoutUrl = withContext(Dispatchers.IO) {
            try {
                createCheckoutUrlProvider(priceId)
            } catch (e: Throwable) {
                Log.e("CheckoutPlugin", "Failed to create checkout url.", e)
                resultCallback?.onResult(InAppPurchase.Result.Failed(e))
                return@withContext null
            }
        } ?: return

        launcher?.launch(checkoutUrl)
    }

    private object CheckoutContract: ActivityResultContract<String, InAppPurchase.Result>() {
        override fun createIntent(context: Context, input: String): Intent {
            return CheckoutForegroundActivity.createIntent(context, input)
        }

        override fun parseResult(resultCode: Int, intent: Intent?): InAppPurchase.Result {
            return when (resultCode) {
                CheckoutForegroundActivity.RESULT_FAILURE -> {
                    val exception = intent?.extras?.let {
                        BundleCompat.getSerializable(
                            it,
                            CheckoutForegroundActivity.EXTRA_FAILURE,
                            Exception::class.java
                        )
                    }
                    if (exception != null) {
                        InAppPurchase.Result.Failed(
                            error = exception,
                        )
                    } else {
                        InAppPurchase.Result.Canceled()
                    }
                }

                CheckoutForegroundActivity.RESULT_COMPLETE -> {
                    InAppPurchase.Result.Completed()
                }

                Activity.RESULT_CANCELED -> {
                    InAppPurchase.Result.Canceled()
                }
                else -> {
                    InAppPurchase.Result.Canceled()
                }
            }
        }
    }
}
