package com.stripe.android.paymentelement.confirmation.shoppay

import com.stripe.android.paymentelement.confirmation.ConfirmationDefinition
import dagger.Binds
import dagger.Module
import dagger.multibindings.IntoSet

@Module
internal interface ShopPayConfirmationModule {
    @JvmSuppressWildcards
    @Binds
    @IntoSet
    fun bindsShopPayConfirmationDefinition(
        definition: ShopPayConfirmationDefinition
    ): ConfirmationDefinition<*, *, *, *>
}
