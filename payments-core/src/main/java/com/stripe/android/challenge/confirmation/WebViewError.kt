package com.stripe.android.challenge.confirmation

internal class WebViewError(
    override val message: String?,
    val url: String?,
    val errorCode: Int?,
    val webViewErrorType: String
) : Throwable()
