package com.stripe.android.connect.webview.serialization

import kotlinx.serialization.Serializable

@Serializable
internal data class AlertJs(
    val title: String? = null,
    val message: String? = null,
    val buttons: ButtonsJs? = null,
) {
    @Serializable
    internal data class ButtonsJs(
        val ok: String? = null,
        val cancel: String? = null,
    )
}
