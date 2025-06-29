package com.stripe.android.iapexample

import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.Column
import androidx.compose.material.Button
import androidx.compose.material.Text
import com.stripe.android.iap.InAppPurchase
import com.stripe.android.iap.InAppPurchasePluginLookupType

internal class MainActivity : AppCompatActivity(), InAppPurchase.ResultCallback {

    companion object {
        private const val EXAMPLE_PRICE_ID = "price_1OMWl7Lu5o3P18ZpdSyfgjmd"
    }

    private val viewModel by viewModels<MainActivityViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val inAppPurchase = InAppPurchase(
            publishableKey = "pk_test_123",
            activity = this,
            resultCallback = this,
            plugins = viewModel.plugins,
        )

        setContent {
            Column {
                Button(
                    onClick = { inAppPurchase.purchase(EXAMPLE_PRICE_ID) }
                ) {
                    Text("Default")
                }

                Button(
                    onClick = {
                        inAppPurchase.purchase(EXAMPLE_PRICE_ID, lookupType = InAppPurchasePluginLookupType.Checkout())
                    }
                ) {
                    Text("Checkout")
                }

                Button(
                    onClick = {
                        inAppPurchase.purchase(EXAMPLE_PRICE_ID, lookupType = InAppPurchasePluginLookupType.GooglePlay())
                    }
                ) {
                    Text("Google Play")
                }
            }
        }
    }

    override fun onResult(result: InAppPurchase.Result) {
        when (result) {
            is InAppPurchase.Result.Canceled -> {
                Toast.makeText(this, "Canceled", Toast.LENGTH_LONG).show()
            }
            is InAppPurchase.Result.Completed -> {
                // Just call your server, given the authenticated customer, and look up subscription
                Toast.makeText(this, "Completed", Toast.LENGTH_LONG).show()
            }
            is InAppPurchase.Result.Failed -> {
                Toast.makeText(this, "Failed", Toast.LENGTH_LONG).show()
            }
        }
    }
}
