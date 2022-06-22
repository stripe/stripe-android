package com.stripe.android.ui.core.injection

import android.content.Context
import androidx.annotation.RestrictTo
import com.stripe.android.model.PaymentIntent
import com.stripe.android.model.StripeIntent
import com.stripe.android.ui.core.Amount
import com.stripe.android.ui.core.elements.IdentifierSpec
import com.stripe.android.ui.core.forms.TransformSpecToElements
import com.stripe.android.ui.core.forms.resources.ResourceRepository
import dagger.Module
import dagger.Provides

@Module
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
abstract class FormControllerModule {

    companion object {

        @Provides
        fun provideTransformSpecToElements(
            resourceRepository: ResourceRepository,
            context: Context,
            merchantName: String,
            stripeIntent: StripeIntent,
            initialValues: Map<IdentifierSpec, String?>,
            viewOnlyFields: Set<IdentifierSpec>
        ) = TransformSpecToElements(
            resourceRepository = resourceRepository,
            initialValues = initialValues,
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
            viewOnlyFields = viewOnlyFields
        )
    }
}
