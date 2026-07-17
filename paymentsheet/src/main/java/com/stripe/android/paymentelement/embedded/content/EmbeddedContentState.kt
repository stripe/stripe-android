package com.stripe.android.paymentelement.embedded.content

import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata
import com.stripe.android.paymentsheet.PaymentSheet.Appearance.Embedded

/**
 * The narrowed slice of the confirmation state that [DefaultEmbeddedContentHelperDataSource] renders
 * its content from, projected from [EmbeddedContentHelperDataSource.embeddedConfirmationState]. It
 * is a `data class` so structural equality lets the derived content flow dedupe: a selection-only
 * change to the confirmation state projects to an equal [EmbeddedContentState], so the content
 * isn't rebuilt.
 */
internal data class EmbeddedContentState(
    val paymentMethodMetadata: PaymentMethodMetadata,
    val appearance: Embedded,
    val embeddedViewDisplaysMandateText: Boolean,
)
