package com.stripe.android.paymentelement.confirmation.cardart

import com.stripe.android.paymentelement.confirmation.ConfirmationDefinition
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

    companion object {
        @OptIn(DelicateCoroutinesApi::class)
        @Provides
        @PaymentOptionCardArtPrefetchScope
        fun providesCoroutineScope(): CoroutineScope = GlobalScope
    }
}
