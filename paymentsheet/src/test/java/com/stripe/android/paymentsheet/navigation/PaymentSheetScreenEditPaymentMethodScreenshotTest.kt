package com.stripe.android.paymentsheet.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadataFactory
import com.stripe.android.model.PaymentMethodFixtures
import com.stripe.android.paymentsheet.FakeEditPaymentMethodInteractor
import com.stripe.android.paymentsheet.navigation.PaymentSheetScreen.EditPaymentMethod
import com.stripe.android.paymentsheet.ui.PaymentSheetFlowType
import com.stripe.android.paymentsheet.ui.PaymentSheetScreen
import com.stripe.android.paymentsheet.viewmodels.FakeBaseSheetViewModel
import com.stripe.android.screenshottesting.PaparazziRule
import com.stripe.android.testing.CoroutineTestRule
import org.junit.Rule
import org.junit.Test

internal class PaymentSheetScreenEditPaymentMethodScreenshotTest {
    @get:Rule
    val paparazziRule = PaparazziRule(
        boxModifier = Modifier
            .padding(16.dp)
    )

    @get:Rule
    val coroutineRule = CoroutineTestRule()

    @Test
    fun displaysUpdateCardScreen() {
        val metadata = PaymentMethodMetadataFactory.create()
        val interactor = FakeEditPaymentMethodInteractor(PaymentMethodFixtures.CARD_PAYMENT_METHOD)
        val initialScreen = EditPaymentMethod(interactor)
        val viewModel = FakeBaseSheetViewModel.create(metadata, initialScreen)

        paparazziRule.snapshot {
            PaymentSheetScreen(viewModel = viewModel, type = PaymentSheetFlowType.Complete)
        }
    }
}
