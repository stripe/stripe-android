package com.stripe.android.connect.webview.serialization

import kotlinx.serialization.Serializable

@Serializable
internal data class AlertJs(
    val title: String?,
    val message: String?,
    val buttons: ButtonsJs?,
) {
    @Serializable
    internal data class ButtonsJs(
        val ok: String?,
        val cancel: String?,
    )
}
