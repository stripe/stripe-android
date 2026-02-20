package com.stripe.android.paymentsheet.verticalmode

import androidx.annotation.RestrictTo
import androidx.lifecycle.SavedStateHandle
import com.stripe.android.common.spms.DefaultLinkFormElementFactory
import com.stripe.android.common.spms.DefaultSavedPaymentMethodLinkFormHelper
import com.stripe.android.common.spms.SavedPaymentMethodLinkFormHelper
import com.stripe.android.core.strings.ResolvableString
import com.stripe.android.core.strings.orEmpty
import com.stripe.android.link.LinkConfigurationCoordinator
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata
import com.stripe.android.model.PaymentMethod
import com.stripe.android.paymentsheet.DisplayableSavedPaymentMethod
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.uicore.elements.FormElement

internal interface SavedPaymentMethodConfirmInteractor {
    val displayableSavedPaymentMethod: DisplayableSavedPaymentMethod

    val formElement: FormElement?
}

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
internal class DefaultSavedPaymentMethodConfirmInteractor(
    val initialSelection: PaymentSelection.Saved,
    val displayName: ResolvableString,
    val savedPaymentMethodLinkFormHelper: SavedPaymentMethodLinkFormHelper,
) : SavedPaymentMethodConfirmInteractor {

    override val formElement = savedPaymentMethodLinkFormHelper.formElement

    override val displayableSavedPaymentMethod = DisplayableSavedPaymentMethod.create(
        displayName = displayName,
        paymentMethod = initialSelection.paymentMethod,
    )

    companion object {
        fun create(
            paymentMethodMetadata: PaymentMethodMetadata,
            initialSelection: PaymentSelection.Saved,
            linkConfigurationCoordinator: LinkConfigurationCoordinator,
            savedStateHandle: SavedStateHandle,
        ): DefaultSavedPaymentMethodConfirmInteractor {
            return DefaultSavedPaymentMethodConfirmInteractor(
                initialSelection = initialSelection,
                displayName = paymentMethodMetadata.supportedPaymentMethodForCode(
                    PaymentMethod.Type.Card.code
                )?.displayName.orEmpty(),
                savedPaymentMethodLinkFormHelper = DefaultSavedPaymentMethodLinkFormHelper(
                    paymentMethodMetadata = paymentMethodMetadata,
                    linkConfigurationCoordinator = linkConfigurationCoordinator,
                    savedStateHandle = savedStateHandle,
                    linkFormElementFactory = DefaultLinkFormElementFactory,
                ),
            )
        }
    }
}
