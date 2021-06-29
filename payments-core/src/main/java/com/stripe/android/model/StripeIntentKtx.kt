package com.stripe.android.model

import com.stripe.android.StripePaymentController

fun StripeIntent.getRequestCode() = StripePaymentController.getRequestCode(this)
