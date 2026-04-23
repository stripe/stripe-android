package com.stripe.android.common.taptoadd

internal class TapToAddCardNotSupportedException : Exception(
    "Payment method is not supported by card brand filter!"
)
