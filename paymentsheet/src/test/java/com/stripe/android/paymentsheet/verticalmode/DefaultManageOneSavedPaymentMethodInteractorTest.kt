package com.stripe.android.paymentsheet.verticalmode

import com.google.common.truth.Truth.assertThat
import com.stripe.android.core.strings.resolvableString
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadataFactory
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.PaymentMethodFixtures
import kotlin.test.Test

class DefaultManageOneSavedPaymentMethodInteractorTest {

    @Test
    fun handleViewAction_DeletePaymentMethod_deletesPmAndNavigatesBack() {
        var deletedPm: PaymentMethod? = null
        var hasNavigatedBack = false

        val paymentMethod = PaymentMethodFixtures.CARD_PAYMENT_METHOD
        val interactor = DefaultManageOneSavedPaymentMethodInteractor(
            paymentMethod = paymentMethod,
            paymentMethodMetadata = PaymentMethodMetadataFactory.create(),
            providePaymentMethodName = { it!!.resolvableString },
            onDeletePaymentMethod = { deletedPm = it },
            navigateBack = { hasNavigatedBack = true },
            defaultPaymentMethodId = null
        )

        interactor.handleViewAction(ManageOneSavedPaymentMethodInteractor.ViewAction.DeletePaymentMethod)

        assertThat(deletedPm).isEqualTo(paymentMethod)
        assertThat(hasNavigatedBack).isTrue()
    }
}
