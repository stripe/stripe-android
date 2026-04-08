package com.stripe.android.paymentelement.confirmation.cardart

import com.stripe.android.paymentelement.confirmation.ConfirmationDefinition
import com.stripe.android.paymentsheet.DefaultPaymentOptionCardArtProvider
import com.stripe.android.paymentsheet.PaymentOptionCardArtProvider
import com.stripe.android.uicore.image.ImageOptimizer
import com.stripe.android.uicore.image.StripeCdnImageOptimizer
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.multibindings.IntoSet
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope

@Module
internal interface PaymentOptionCardArtPrefetchConfirmationModule {
    @JvmSuppressWildcards
    @Binds
    @IntoSet
    fun bindsCardArtPrefetchConfirmationDefinition(
        definition: PaymentOptionCardArtPrefetchConfirmationDefinition
    ): ConfirmationDefinition<*, *, *, *>

    @Binds
    fun bindsPaymentOptionCardArtProvider(
        impl: DefaultPaymentOptionCardArtProvider
    ): PaymentOptionCardArtProvider

    companion object {
        @OptIn(DelicateCoroutinesApi::class)
        @Provides
        @PaymentOptionCardArtPrefetchScope
        fun providesCoroutineScope(): CoroutineScope = GlobalScope

        @Provides
        fun providesImageOptimizer(): ImageOptimizer {
            return StripeCdnImageOptimizer
        }
    }
}
