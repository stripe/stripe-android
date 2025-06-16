package com.stripe.android.shoppay

import androidx.activity.result.ActivityResultCaller
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.ActivityResultRegistry
import com.stripe.android.paymentsheet.PaymentSheet
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
            callback(shopPayActivityResult)
        }
    }

    fun register(
        activityResultCaller: ActivityResultCaller,
        callback: (ShopPayActivityResult) -> Unit,
    ) {
        shopPayActivityResultLauncher = activityResultCaller.registerForActivityResult(
            shopPayActivityContract
        ) { shopPayActivityResult ->
            callback(shopPayActivityResult)
        }
    }

    fun unregister() {
        shopPayActivityResultLauncher?.unregister()
        shopPayActivityResultLauncher = null
    }

    fun present(
        shopPayConfiguration: PaymentSheet.ShopPayConfiguration
    ) {
        val args = ShopPayActivityContract.Args(shopPayConfiguration)
        shopPayActivityResultLauncher?.launch(args)
    }
}
