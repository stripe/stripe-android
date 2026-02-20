package com.stripe.android.paymentsheet.verticalmode

import androidx.annotation.RestrictTo
import com.stripe.android.core.strings.ResolvableString
import com.stripe.android.core.strings.orEmpty
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata
import com.stripe.android.model.PaymentMethod
import com.stripe.android.paymentsheet.DisplayableSavedPaymentMethod
import com.stripe.android.paymentsheet.model.PaymentSelection

internal interface SavedPaymentMethodConfirmInteractor {
    val displayableSavedPaymentMethod: DisplayableSavedPaymentMethod
}

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
internal class DefaultSavedPaymentMethodConfirmInteractor(
    val initialSelection: PaymentSelection.Saved,
    val displayName: ResolvableString,
) : SavedPaymentMethodConfirmInteractor {

    override val displayableSavedPaymentMethod = DisplayableSavedPaymentMethod.create(
        displayName = displayName,
        paymentMethod = initialSelection.paymentMethod,
    )

    companion object {
        fun create(
            paymentMethodMetadata: PaymentMethodMetadata,
            initialSelection: PaymentSelection.Saved,
        ): DefaultSavedPaymentMethodConfirmInteractor {
            return DefaultSavedPaymentMethodConfirmInteractor(
                initialSelection = initialSelection,
                displayName = paymentMethodMetadata.supportedPaymentMethodForCode(
                    PaymentMethod.Type.Card.code
                )?.displayName.orEmpty(),
            )
        }
    }
}
