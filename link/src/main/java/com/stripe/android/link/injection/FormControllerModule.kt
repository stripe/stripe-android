package com.stripe.android.link.injection

import android.content.Context
import com.stripe.android.link.LinkActivityContract
import com.stripe.android.model.PaymentIntent
import com.stripe.android.ui.core.Amount
import com.stripe.android.ui.core.elements.IdentifierSpec
import com.stripe.android.ui.core.forms.TransformSpecToElements
import com.stripe.android.ui.core.forms.resources.ResourceRepository
import dagger.Module
import dagger.Provides

@Module
internal abstract class FormControllerModule {

    companion object {

        @Provides
        fun provideTransformSpecToElements(
            resourceRepository: ResourceRepository,
            context: Context,
            starterArgs: LinkActivityContract.Args,
            initialValues: Map<IdentifierSpec, String?>,
            viewOnlyFields: Set<IdentifierSpec>
        ) = TransformSpecToElements(
            resourceRepository = resourceRepository,
            initialValues = initialValues,
            amount = (starterArgs.stripeIntent as? PaymentIntent)?.let {
                val amount = it.amount
                val currency = it.currency
                if (amount != null && currency != null) {
                    Amount(amount, currency)
                }
                null
            },
            saveForFutureUseInitialValue = false,
            merchantName = starterArgs.merchantName,
            context = context,
            viewOnlyFields = viewOnlyFields
        )
    }
}
