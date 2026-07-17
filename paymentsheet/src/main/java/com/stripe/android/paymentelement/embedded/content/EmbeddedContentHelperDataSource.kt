package com.stripe.android.paymentelement.embedded.content

import com.stripe.android.core.injection.ViewModelScope
import com.stripe.android.paymentelement.EmbeddedPaymentElement
import com.stripe.android.paymentsheet.ui.WalletButtonsContent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Supplies everything [DefaultEmbeddedContentHelper] renders: the raw [embeddedConfirmationState]
 * (which the wallet buttons and sheet-launch args still read directly) and the derived
 * [embeddedContent] / [walletButtonsContent] built from it. The content-building lives in
 * [EmbeddedContentBuilder] rather than in the helper so each integration
 * ([EmbeddedPaymentElement][com.stripe.android.paymentelement.EmbeddedPaymentElement] today, checkout
 * later) can supply this one abstraction while the helper stays a thin facade over it.
 */
internal interface EmbeddedContentHelperDataSource {
    val embeddedConfirmationState: StateFlow<EmbeddedConfirmationStateHolder.State?>
    val embeddedContent: StateFlow<EmbeddedContent?>
    val walletButtonsContent: StateFlow<WalletButtonsContent?>
}

@Singleton
internal class DefaultEmbeddedContentHelperDataSource @Inject constructor(
    @ViewModelScope private val coroutineScope: CoroutineScope,
    private val confirmationStateHolder: EmbeddedConfirmationStateHolder,
    private val contentBuilder: EmbeddedContentBuilder,
) : EmbeddedContentHelperDataSource {

    override val embeddedConfirmationState: StateFlow<EmbeddedConfirmationStateHolder.State?> =
        confirmationStateHolder.stateFlow

    private val contentFlows: EmbeddedContentBuilder.ContentFlows = contentBuilder.build(
        coroutineScope = coroutineScope,
        dataSource = this,
        embeddedConfirmationState = embeddedConfirmationState,
    )

    override val embeddedContent: StateFlow<EmbeddedContent?> = contentFlows.embeddedContent

    override val walletButtonsContent: StateFlow<WalletButtonsContent?> = contentFlows.walletButtonsContent
}
