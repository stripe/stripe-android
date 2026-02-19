package com.stripe.android.paymentelement.embedded.form

import com.stripe.android.link.LinkConfiguration
import com.stripe.android.link.LinkConfigurationCoordinator
import com.stripe.android.link.ui.inline.UserInput
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata
import com.stripe.android.model.PaymentMethod
import com.stripe.android.paymentsheet.verticalmode.DefaultSavedCardConfirmInteractor
import com.stripe.android.paymentsheet.verticalmode.SavedCardConfirmInteractor
import javax.inject.Inject

internal class EmbeddedSavedCardConfirmInteractorFactory @Inject constructor(
    private val paymentMethodMetadata: PaymentMethodMetadata,
    private val linkConfigurationCoordinator: LinkConfigurationCoordinator,
) : SavedCardConfirmInteractor.Factory {

    override fun create(
        paymentMethod: PaymentMethod,
        onUserInputChanged: (UserInput?) -> Unit
    ): SavedCardConfirmInteractor {
        return DefaultSavedCardConfirmInteractor.Factory(
            paymentMethodMetadata = paymentMethodMetadata,
            linkConfiguration = null,
            linkConfigurationCoordinator = linkConfigurationCoordinator,
        ).create(paymentMethod, onUserInputChanged)
    }
}