package com.stripe.android.stripe3ds2.security

class MessageTransformerFactory(
    private val isLiveMode: Boolean
) {
    fun create(): MessageTransformer {
        return DefaultMessageTransformer(isLiveMode)
    }
}
