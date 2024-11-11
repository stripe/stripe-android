package com.stripe.android.paymentsheet.addresselement

import android.content.Context
import com.stripe.android.DefaultCardBrandFilter
import com.stripe.android.cards.DefaultCardAccountRangeRepositoryFactory
import com.stripe.android.core.injection.INITIAL_VALUES
import com.stripe.android.core.injection.SHIPPING_VALUES
import com.stripe.android.lpmfoundations.luxe.TransformSpecToElements
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.uicore.elements.IdentifierSpec
import dagger.Module
import dagger.Provides
import javax.inject.Named

@Module
internal object FormControllerModule {
    @Provides
    fun provideTransformSpecToElements(
        merchantName: String,
        @Named(INITIAL_VALUES) initialValues: Map<IdentifierSpec, String?>,
        @Named(SHIPPING_VALUES) shippingValues: Map<IdentifierSpec, String?>?
    ) = TransformSpecToElements(
        arguments = TransformSpecToElements.Arguments(
            initialValues = initialValues,
            shippingValues = shippingValues,
            merchantName = merchantName,
            billingDetailsCollectionConfiguration = PaymentSheet.BillingDetailsCollectionConfiguration(),
            requiresMandate = false,
        )
    )
}
