package com.stripe.android.iapexample

import androidx.lifecycle.ViewModel
import com.stripe.android.iap.InAppPurchasePlugin
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory

internal class MainActivityViewModel : ViewModel() {
    private val backendService: BackendService = Retrofit.Builder()
//        .baseUrl("https://stp-mobile-playground-backend-v7.stripedemos.com/")
        .baseUrl("http://10.0.2.2:8081/")
        .addConverterFactory(Json.asConverterFactory("application/json".toMediaType()))
        .build()
        .create(BackendService::class.java)

    val plugins = listOf(
        InAppPurchasePlugin.stripeCheckout { priceId ->
            backendService.createCheckoutSessionUrl(CreateRequest(priceId)).url
        },
        InAppPurchasePlugin.googlePlay { priceId ->
            backendService.createIapCustomerSession(CreateRequest(priceId)).customerSessionClientSecret
        },
    )
}
