package com.stripe.android.paymentelement.confirmation.cardart

import com.stripe.android.core.injection.IOContext
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata
import com.stripe.android.paymentelement.confirmation.BootstrapOnlyConfirmationDefinition
import com.stripe.android.uicore.image.StripeImageLoader
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.coroutines.CoroutineContext

internal class CardArtPrefetchConfirmationDefinition @Inject constructor(
    @CardArtPrefetchScope private val imageLoader: StripeImageLoader,
    @CardArtPrefetchScope private val coroutineScope: CoroutineScope,
    @IOContext private val workContext: CoroutineContext,
) : BootstrapOnlyConfirmationDefinition() {

    override val key: String = "CardArtPrefetch"

    override fun bootstrap(paymentMethodMetadata: PaymentMethodMetadata) {
        val urls = paymentMethodMetadata.cardArtUrls
        if (urls.isEmpty()) return

        for (url in urls) {
            coroutineScope.launch(workContext) { imageLoader.load(url) }
        }
    }
}
