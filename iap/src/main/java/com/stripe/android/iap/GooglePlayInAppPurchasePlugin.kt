package com.stripe.android.iap

import android.app.Activity
import androidx.activity.result.ActivityResultCaller
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClient.ProductType
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryProductDetailsParams.Product
import com.android.billingclient.api.queryProductDetails
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

internal class GooglePlayInAppPurchasePlugin(
    private val customerSessionClientSecretProvider: suspend (priceId: String) -> String
) : InAppPurchasePlugin() {
    private var launcher: Launcher? = null

    override fun register(
        activity: Activity,
        activityResultCaller: ActivityResultCaller,
        resultCallback: InAppPurchase.ResultCallback
    ) {
        val billingClient = BillingClient.newBuilder(activity)
            .setListener { result, purchaseList ->
//                if (result.responseCode)
//                resultCallback.onResult()
            }
            .enablePendingPurchases(PendingPurchasesParams.newBuilder().enableOneTimeProducts().build())
            .build()

        launcher = Launcher(billingClient, activity)
    }

    override fun unregister() {
        launcher = null
    }

    override val analyticsName: String = "GooglePlay"


    override suspend fun purchase(priceId: String) {
        launcher?.launch()
    }

    private class Launcher(
        private val billingClient: BillingClient,
        private val activity: Activity,
    ) {
        suspend fun launch() {
            val queryProductDetailsParams = QueryProductDetailsParams.newBuilder()
                .setProductList(
                    listOf(
                        Product.newBuilder()
                            .setProductId("product_id_example")
                            .setProductType(ProductType.SUBS)
                            .build()
                    )
                )
                .build()

            val productDetailsResult = withContext(Dispatchers.IO) {
                billingClient.queryProductDetails(queryProductDetailsParams)
            }

            val productDetailsParamsList = listOf(
                BillingFlowParams.ProductDetailsParams.newBuilder()
                    // TODO:
                    .setProductDetails(productDetailsResult.productDetailsList!!.first())
                    .build()
            )

            val billingFlowParams = BillingFlowParams.newBuilder()
                .setProductDetailsParamsList(productDetailsParamsList)
                .build()

            billingClient.launchBillingFlow(activity, billingFlowParams)
        }
    }
}
