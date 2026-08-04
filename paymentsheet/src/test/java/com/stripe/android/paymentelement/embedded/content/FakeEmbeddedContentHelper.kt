package com.stripe.android.paymentelement.embedded.content

import app.cash.turbine.Turbine
import kotlinx.coroutines.flow.MutableStateFlow

internal class FakeEmbeddedContentHelper(
    override val embeddedContent: MutableStateFlow<EmbeddedContent?> = MutableStateFlow(null),
) : EmbeddedContentHelper {
    val presentPaymentOptionsCalls: Turbine<Unit> = Turbine()

    override fun presentPaymentOptions() {
        presentPaymentOptionsCalls.add(Unit)
    }
}
