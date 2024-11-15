package com.stripe.android.paymentsheet.ui

import androidx.compose.runtime.Composable
import com.stripe.android.core.strings.resolvableString
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

    @Test
    fun updatePaymentMethodScreen_forCard_withRemoveButton_withError() {
        paparazziRule.snapshot {
            PaymentSheetScreenOnUpdatePaymentMethod(
                canRemove = true,
                error = "Something went wrong",
            )
        }
    }

    @Composable
    fun PaymentSheetScreenOnUpdatePaymentMethod(
        canRemove: Boolean,
        error: String? = null,
    ) {
        val paymentMethod = PaymentMethodFactory.visaCard().toDisplayableSavedPaymentMethod()
        val interactor = FakeUpdatePaymentMethodInteractor(
            displayableSavedPaymentMethod = paymentMethod,
            canRemove = canRemove,
            viewActionRecorder = null,
            initialState = UpdatePaymentMethodInteractor.State(
                error = error?.resolvableString,
                isRemoving = false,
            ),
        )
        val screen = com.stripe.android.paymentsheet.navigation.PaymentSheetScreen.UpdatePaymentMethod(interactor)
        val metadata = PaymentMethodMetadataFactory.create()
        val viewModel = FakeBaseSheetViewModel.create(metadata, screen)

        PaymentSheetScreen(viewModel = viewModel, type = PaymentSheetFlowType.Complete)
    }
}
