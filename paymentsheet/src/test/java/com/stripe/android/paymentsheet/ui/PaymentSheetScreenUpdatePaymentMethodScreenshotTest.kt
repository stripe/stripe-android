package com.stripe.android.paymentsheet.ui

import androidx.compose.runtime.Composable
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadataFactory
import com.stripe.android.model.PaymentMethodFixtures.toDisplayableSavedPaymentMethod
import com.stripe.android.paymentsheet.viewmodels.FakeBaseSheetViewModel
import com.stripe.android.screenshottesting.PaparazziRule
import com.stripe.android.testing.PaymentMethodFactory
import org.junit.Rule
import org.junit.Test

class PaymentSheetScreenUpdatePaymentMethodScreenshotTest {
    @get:Rule
    val paparazziRule = PaparazziRule()

    @Test
    fun updatePaymentMethodScreen_forCard() {
        paparazziRule.snapshot {
            PaymentSheetScreenOnUpdatePaymentMethod(canRemove = false)
        }
    }

    @Test
    fun updatePaymentMethodScreen_forCard_withRemoveButton() {
        paparazziRule.snapshot {
            PaymentSheetScreenOnUpdatePaymentMethod(canRemove = true)
        }
    }

    @Composable
    fun PaymentSheetScreenOnUpdatePaymentMethod(
        canRemove: Boolean,
    ) {
        val paymentMethod = PaymentMethodFactory.visaCard().toDisplayableSavedPaymentMethod()
        val interactor = DefaultUpdatePaymentMethodInteractor(
            isLiveMode = true,
            displayableSavedPaymentMethod = paymentMethod,
            card = paymentMethod.paymentMethod.card!!,
            onRemovePaymentMethod = {},
            navigateBack = {},
            canRemove = canRemove,
        )
        val screen = com.stripe.android.paymentsheet.navigation.PaymentSheetScreen.UpdatePaymentMethod(interactor)
        val metadata = PaymentMethodMetadataFactory.create()
        val viewModel = FakeBaseSheetViewModel.create(metadata, screen)

        PaymentSheetScreen(viewModel = viewModel, type = PaymentSheetFlowType.Complete)
    }
}
