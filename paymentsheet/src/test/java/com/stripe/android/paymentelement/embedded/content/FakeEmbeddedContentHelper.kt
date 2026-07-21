package com.stripe.android.paymentelement.embedded.content

import kotlinx.coroutines.flow.MutableStateFlow

internal class FakeEmbeddedContentHelper(
    override val embeddedContent: MutableStateFlow<EmbeddedContent?> = MutableStateFlow(null),
) : EmbeddedContentHelper {
    override fun presentPaymentOptions() = Unit
}
