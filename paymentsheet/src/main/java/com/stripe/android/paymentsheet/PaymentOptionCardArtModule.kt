package com.stripe.android.paymentsheet

import com.stripe.android.uicore.image.ImageOptimizer
import com.stripe.android.uicore.image.StripeCdnImageOptimizer
import dagger.Binds
import dagger.Module
import dagger.Provides

@Module
internal interface PaymentOptionCardArtModule {
    @Binds
    fun bindsPaymentOptionCardArtProvider(
        impl: DefaultPaymentOptionCardArtProvider
    ): PaymentOptionCardArtProvider

    @Binds
    fun bindsPaymentOptionCardArtDrawableLoader(
        impl: DefaultPaymentOptionCardArtDrawableLoader
    ): PaymentOptionCardArtDrawableLoader

    companion object {
        @Provides
        fun providesImageOptimizer(): ImageOptimizer = StripeCdnImageOptimizer
    }
}
