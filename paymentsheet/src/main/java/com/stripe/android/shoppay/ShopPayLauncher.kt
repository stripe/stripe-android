package com.stripe.android.shoppay

import android.util.Log
import androidx.activity.result.ActivityResultCaller
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.ActivityResultRegistry
import com.stripe.android.paymentsheet.WalletConfiguration
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class ShopPayLauncher @Inject internal constructor(
    private val shopPayActivityContract: ShopPayActivityContract
) {

    private var shopPayActivityResultLauncher:
        ActivityResultLauncher<ShopPayActivityContract.Args>? = null

    fun register(
        activityResultRegistry: ActivityResultRegistry,
        callback: (ShopPayActivityResult) -> Unit,
    ) {
        shopPayActivityResultLauncher = activityResultRegistry.register(
            "ShopPayLauncher",
            shopPayActivityContract,
        ) { shopPayActivityResult ->
            handleActivityResult(shopPayActivityResult, callback)
        }
    }

    fun register(
        activityResultCaller: ActivityResultCaller,
        callback: (ShopPayActivityResult) -> Unit,
    ) {
        shopPayActivityResultLauncher = activityResultCaller.registerForActivityResult(
            shopPayActivityContract
        ) { shopPayActivityResult ->
            handleActivityResult(shopPayActivityResult, callback)
        }
    }

    private fun handleActivityResult(
        shopPayActivityResult: ShopPayActivityResult,
        nextStep: (ShopPayActivityResult) -> Unit
    ) {
        when (shopPayActivityResult) {
            ShopPayActivityResult.Canceled -> Unit
            is ShopPayActivityResult.Completed -> {
                Log.d("ShopPayLauncher", "$shopPayActivityResult")
            }
            is ShopPayActivityResult.Failed -> Unit
        }
        nextStep(shopPayActivityResult)
    }

    fun unregister() {
        shopPayActivityResultLauncher?.unregister()
        shopPayActivityResultLauncher = null
    }

    fun present(
        checkoutUrl: String,
        walletHandlers: WalletConfiguration.Handlers
    ) {
        val args = ShopPayActivityContract.Args(checkoutUrl, walletHandlers)
        shopPayActivityResultLauncher?.launch(args)
    }
}
