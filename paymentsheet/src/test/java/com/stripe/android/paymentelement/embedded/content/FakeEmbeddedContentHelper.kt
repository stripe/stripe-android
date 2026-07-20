package com.stripe.android.paymentelement.embedded.content

import com.stripe.android.paymentsheet.ui.WalletButtonsContent
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

internal class FakeEmbeddedContentHelper(
    override val embeddedContent: MutableStateFlow<EmbeddedContent?> = MutableStateFlow(null),
    override val walletButtonsContent: StateFlow<WalletButtonsContent?> = MutableStateFlow(null),
) : EmbeddedContentHelper {
    override fun presentPaymentOptions() = Unit
}
