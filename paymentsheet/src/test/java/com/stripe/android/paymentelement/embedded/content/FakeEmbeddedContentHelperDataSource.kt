package com.stripe.android.paymentelement.embedded.content

import com.stripe.android.paymentsheet.ui.WalletButtonsContent
import kotlinx.coroutines.flow.MutableStateFlow

internal class FakeEmbeddedContentHelperDataSource(
    override val embeddedConfirmationState: MutableStateFlow<EmbeddedConfirmationStateHolder.State?> =
        MutableStateFlow(null),
    override val embeddedContent: MutableStateFlow<EmbeddedContent?> = MutableStateFlow(null),
    override val walletButtonsContent: MutableStateFlow<WalletButtonsContent?> = MutableStateFlow(null),
) : EmbeddedContentHelperDataSource
