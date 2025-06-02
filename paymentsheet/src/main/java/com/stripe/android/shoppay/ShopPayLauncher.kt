package com.stripe.android.shoppay

import androidx.activity.result.ActivityResultCaller
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.ActivityResultRegistry
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
            is ShopPayActivityResult.Completed -> Unit
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
    ) {
        val args = ShopPayActivityContract.Args(checkoutUrl)
        shopPayActivityResultLauncher?.launch(args)
    }
}
