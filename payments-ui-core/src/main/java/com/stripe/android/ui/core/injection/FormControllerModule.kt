package com.stripe.android.ui.core.injection

import android.content.Context
import androidx.annotation.RestrictTo
import com.stripe.android.core.injection.INITIAL_VALUES
import com.stripe.android.core.injection.SHIPPING_VALUES
import com.stripe.android.model.PaymentIntent
import com.stripe.android.model.StripeIntent
import com.stripe.android.ui.core.Amount
import com.stripe.android.ui.core.cbc.CardBrandChoiceEligibility
import com.stripe.android.ui.core.forms.TransformSpecToElements
import com.stripe.android.uicore.address.AddressRepository
import com.stripe.android.uicore.elements.IdentifierSpec
import dagger.Module
import dagger.Provides
import javax.inject.Named

@Module
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
object FormControllerModule {
    @Provides
    fun provideTransformSpecToElements(
        addressRepository: AddressRepository,
        context: Context,
        merchantName: String,
        stripeIntent: StripeIntent?,
        @Named(INITIAL_VALUES) initialValues: Map<IdentifierSpec, String?>,
        @Named(SHIPPING_VALUES) shippingValues: Map<IdentifierSpec, String?>?
    ) = TransformSpecToElements(
        addressRepository = addressRepository,
        initialValues = initialValues,
        shippingValues = shippingValues,
        amount = (stripeIntent as? PaymentIntent)?.let {
            val amount = it.amount
            val currency = it.currency
            if (amount != null && currency != null) {
                Amount(amount, currency)
            }
            null
        },
        saveForFutureUseInitialValue = false,
        merchantName = merchantName,
        context = context,
        cbcEligibility = CardBrandChoiceEligibility.Ineligible,
    )
}
