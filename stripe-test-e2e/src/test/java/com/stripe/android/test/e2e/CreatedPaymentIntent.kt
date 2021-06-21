package com.stripe.android.test.e2e

import com.squareup.moshi.Json

data class CreatedPaymentIntent(
    @field:Json(name = "publishableKey") val publishableKey: String,
    @field:Json(name = "paymentIntent") val clientSecret: String,
    @field:Json(name = "expectedAmount") val amount: Int,
    @field:Json(name = "expectedCurrency") val currency: String,
    @field:Json(name = "expectedAccountID") val accountId: String
)
