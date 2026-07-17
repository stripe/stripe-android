package com.stripe.android.checkout.injection

import com.stripe.android.checkout.CheckoutSheetLauncher
import com.stripe.android.paymentelement.embedded.content.DefaultEmbeddedContentHelper
import com.stripe.android.paymentelement.embedded.content.EmbeddedContentHelper
import com.stripe.android.paymentelement.embedded.content.EmbeddedSheetLauncher
import dagger.Binds
import dagger.Module

@Module
internal interface PaymentElementModule {
    @Binds
    fun bindsEmbeddedContentHelper(
        helper: DefaultEmbeddedContentHelper
    ): EmbeddedContentHelper

    @Binds
    fun bindsSheetLauncher(
        launcher: CheckoutSheetLauncher
    ): EmbeddedSheetLauncher
}
