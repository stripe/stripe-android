package com.stripe.android.paymentsheet.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.stripe.android.core.strings.resolvableString
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadataFactory
import com.stripe.android.paymentsheet.navigation.PaymentSheetScreen.AddAnotherPaymentMethod
import com.stripe.android.paymentsheet.ui.FakeAddPaymentMethodInteractor
import com.stripe.android.paymentsheet.ui.FakeAddPaymentMethodInteractor.Companion.createState
import com.stripe.android.paymentsheet.ui.PaymentSheetFlowType
import com.stripe.android.paymentsheet.ui.PaymentSheetScreen
import com.stripe.android.paymentsheet.utils.ViewModelStoreOwnerContext
import com.stripe.android.paymentsheet.viewmodels.FakeBaseSheetViewModel
import com.stripe.android.screenshottesting.PaparazziRule
import com.stripe.android.testing.CoroutineTestRule
import org.junit.Rule
import org.junit.Test

internal class PaymentSheetScreenAddAnotherPaymentMethodScreenshotTest {
    @get:Rule
    val paparazziRule = PaparazziRule(
        boxModifier = Modifier
            .padding(16.dp)
    )

    @get:Rule
    val coroutineRule = CoroutineTestRule()

    @Test
    fun displaysCard() {
        val metadata = PaymentMethodMetadataFactory.create()
        val interactor = FakeAddPaymentMethodInteractor(initialState = createState())
        val initialScreen = AddAnotherPaymentMethod(interactor)
        val viewModel = FakeBaseSheetViewModel.create(metadata, initialScreen, canGoBack = true)

        paparazziRule.snapshot {
            ViewModelStoreOwnerContext {
                PaymentSheetScreen(viewModel = viewModel, type = PaymentSheetFlowType.Complete)
            }
        }
    }

    @Test
    fun displaysError() {
        val metadata = PaymentMethodMetadataFactory.create()
        val interactor = FakeAddPaymentMethodInteractor(initialState = createState())
        val initialScreen = AddAnotherPaymentMethod(interactor)
        val viewModel = FakeBaseSheetViewModel.create(metadata, initialScreen, canGoBack = true)
        viewModel.onError("An error occurred.".resolvableString)

        paparazziRule.snapshot {
            ViewModelStoreOwnerContext {
                PaymentSheetScreen(viewModel = viewModel, type = PaymentSheetFlowType.Complete)
            }
        }
    }
}
