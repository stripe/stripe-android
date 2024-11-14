package com.stripe.android.paymentsheet.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.stripe.android.core.strings.resolvableString
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadataFactory
import com.stripe.android.model.PaymentMethodFixtures
import com.stripe.android.paymentsheet.DisplayableSavedPaymentMethod
import com.stripe.android.paymentsheet.navigation.PaymentSheetScreen.ManageSavedPaymentMethods
import com.stripe.android.paymentsheet.ui.PaymentSheetFlowType
import com.stripe.android.paymentsheet.ui.PaymentSheetScreen
import com.stripe.android.paymentsheet.verticalmode.FakeManageScreenInteractor
import com.stripe.android.paymentsheet.verticalmode.ManageScreenInteractor
import com.stripe.android.paymentsheet.viewmodels.FakeBaseSheetViewModel
import com.stripe.android.screenshottesting.PaparazziRule
import com.stripe.android.testing.CoroutineTestRule
import org.junit.Rule
import org.junit.Test

internal class PaymentSheetScreenManageSavedPaymentMethodsScreenshotTest {
    @get:Rule
    val paparazziRule = PaparazziRule(
        boxModifier = Modifier
            .padding(16.dp)
    )

    @get:Rule
    val coroutineRule = CoroutineTestRule()

    private val displayableSavedPaymentMethods =
        listOf(PaymentMethodFixtures.CARD_PAYMENT_METHOD)
            .plus(PaymentMethodFixtures.CARD_WITH_NETWORKS_PAYMENT_METHOD)
            .map {
                DisplayableSavedPaymentMethod(
                    displayName = it.card!!.last4!!.resolvableString,
                    paymentMethod = it,
                    isCbcEligible = true
                )
            }

    @Test
    fun displaysSelectMode() {
        val metadata = PaymentMethodMetadataFactory.create()
        val interactor = FakeManageScreenInteractor(
            initialState = ManageScreenInteractor.State(
                paymentMethods = displayableSavedPaymentMethods,
                currentSelection = displayableSavedPaymentMethods.first(),
                isEditing = false,
                canRemove = true,
                canEdit = true,
            )
        )
        val initialScreen = ManageSavedPaymentMethods(interactor)
        val viewModel = FakeBaseSheetViewModel.create(metadata, initialScreen)

        paparazziRule.snapshot {
            PaymentSheetScreen(viewModel = viewModel, type = PaymentSheetFlowType.Complete)
        }
    }

    @Test
    fun displaysEditMode() {
        val metadata = PaymentMethodMetadataFactory.create()
        val interactor = FakeManageScreenInteractor(
            initialState = ManageScreenInteractor.State(
                paymentMethods = displayableSavedPaymentMethods,
                currentSelection = null,
                isEditing = true,
                canRemove = true,
                canEdit = true,
            )
        )
        val initialScreen = ManageSavedPaymentMethods(interactor)
        val viewModel = FakeBaseSheetViewModel.create(metadata, initialScreen)

        paparazziRule.snapshot {
            PaymentSheetScreen(viewModel = viewModel, type = PaymentSheetFlowType.Complete)
        }
    }
}
