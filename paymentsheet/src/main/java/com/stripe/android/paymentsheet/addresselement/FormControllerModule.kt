package com.stripe.android.paymentsheet.addresselement

import android.content.Context
import com.stripe.android.cards.DefaultCardAccountRangeRepositoryFactory
import com.stripe.android.core.injection.INITIAL_VALUES
import com.stripe.android.core.injection.SHIPPING_VALUES
import com.stripe.android.lpmfoundations.luxe.TransformSpecToElements
import com.stripe.android.lpmfoundations.paymentmethod.UiDefinitionFactory
import com.stripe.android.model.PaymentIntent
import com.stripe.android.model.StripeIntent
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.ui.core.Amount
import com.stripe.android.ui.core.cbc.CardBrandChoiceEligibility
import com.stripe.android.uicore.elements.IdentifierSpec
import dagger.Module
import dagger.Provides
import javax.inject.Named

@Module
internal object FormControllerModule {
    @Provides
    fun provideTransformSpecToElements(
        context: Context,
        merchantName: String,
        stripeIntent: StripeIntent?,
        @Named(INITIAL_VALUES) initialValues: Map<IdentifierSpec, String?>,
        @Named(SHIPPING_VALUES) shippingValues: Map<IdentifierSpec, String?>?
    ) = TransformSpecToElements(
        arguments = UiDefinitionFactory.Arguments(
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
            cardAccountRangeRepositoryFactory = DefaultCardAccountRangeRepositoryFactory(context.applicationContext),
            cbcEligibility = CardBrandChoiceEligibility.Ineligible,
            billingDetailsCollectionConfiguration = PaymentSheet.BillingDetailsCollectionConfiguration(),
            requiresMandate = false,
            linkConfigurationCoordinator = null,
            onLinkInlineSignupStateChanged = {}
        )
    )
}
