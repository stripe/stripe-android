package com.stripe.android.paymentelement.confirmation.cardart

import android.content.Context
import com.stripe.android.paymentelement.confirmation.ConfirmationDefinition
import com.stripe.android.uicore.image.StripeImageLoader
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.multibindings.IntoSet
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope

@Module
internal interface CardArtPrefetchConfirmationModule {
    @JvmSuppressWildcards
    @Binds
    @IntoSet
    fun bindsCardArtPrefetchConfirmationDefinition(
        definition: CardArtPrefetchConfirmationDefinition
    ): ConfirmationDefinition<*, *, *, *>

    companion object {
        @OptIn(DelicateCoroutinesApi::class)
        @Provides
        @CardArtPrefetchScope
        fun providesCoroutineScope(): CoroutineScope = GlobalScope

        @Provides
        @CardArtPrefetchScope
        fun providesStripeImageLoader(context: Context): StripeImageLoader {
            return StripeImageLoader(context)
        }
    }
}
