package com.stripe.android.paymentsheet.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.stripe.android.core.strings.resolvableString
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadataFactory
import com.stripe.android.model.PaymentMethodFixtures
import com.stripe.android.model.PaymentMethodFixtures.toDisplayableSavedPaymentMethod
import com.stripe.android.paymentsheet.DisplayableSavedPaymentMethod
import com.stripe.android.paymentsheet.FakeSelectSavedPaymentMethodsInteractor
import com.stripe.android.paymentsheet.PaymentOptionsItem
import com.stripe.android.paymentsheet.navigation.PaymentSheetScreen.SelectSavedPaymentMethods
import com.stripe.android.paymentsheet.ui.PaymentSheetFlowType
import com.stripe.android.paymentsheet.ui.PaymentSheetScreen
import com.stripe.android.paymentsheet.ui.SelectSavedPaymentMethodsInteractor
import com.stripe.android.paymentsheet.viewmodels.FakeBaseSheetViewModel
import com.stripe.android.screenshottesting.PaparazziRule
import com.stripe.android.testing.CoroutineTestRule
import kotlinx.coroutines.flow.update
import org.junit.Rule
import org.junit.Test

internal class PaymentSheetScreenSelectSavedPaymentMethodsScreenshotTest {
    @get:Rule
    val paparazziRule = PaparazziRule(
        boxModifier = Modifier
            .padding(16.dp)
    )

    @get:Rule
    val coroutineRule = CoroutineTestRule()

    private val savedPaymentOptionItem = PaymentOptionsItem.SavedPaymentMethod(
        displayableSavedPaymentMethod = DisplayableSavedPaymentMethod.create(
            displayName = "Card 4242".resolvableString,
            paymentMethod = PaymentMethodFixtures.CARD_PAYMENT_METHOD,
            isCbcEligible = true,
        ),
    )

    private val defaultSavedPaymentOptionItem = PaymentOptionsItem.SavedPaymentMethod(
        displayableSavedPaymentMethod = DisplayableSavedPaymentMethod.create(
            displayName = "Card 5555".resolvableString,
            paymentMethod = PaymentMethodFixtures.CARD_PAYMENT_METHOD,
            shouldShowDefaultBadge = true,
            isCbcEligible = true,
        ),
    )

    @Test
    fun displaysSelectSavedPaymentMethods() {
        val metadata = PaymentMethodMetadataFactory.create()
        val interactor = FakeSelectSavedPaymentMethodsInteractor(
            initialState = SelectSavedPaymentMethodsInteractor.State(
                paymentOptionsItems = listOf(
                    PaymentOptionsItem.AddCard,
                    savedPaymentOptionItem,
                ),
                selectedPaymentOptionsItem = savedPaymentOptionItem,
                isEditing = false,
                isProcessing = false,
                canEdit = true,
                canRemove = true,
            )
        )
        val initialScreen = SelectSavedPaymentMethods(interactor)
        val viewModel = FakeBaseSheetViewModel.create(metadata, initialScreen)
        viewModel.primaryButtonUiStateSource.update { original ->
            original?.copy(enabled = true)
        }

        paparazziRule.snapshot {
            PaymentSheetScreen(viewModel = viewModel, type = PaymentSheetFlowType.Complete)
        }
    }

    @Test
    fun displaysSelectSavedPaymentMethodsInEditMode() {
        val metadata = PaymentMethodMetadataFactory.create()
        val interactor = FakeSelectSavedPaymentMethodsInteractor(
            initialState = SelectSavedPaymentMethodsInteractor.State(
                paymentOptionsItems = listOf(
                    PaymentOptionsItem.AddCard,
                    savedPaymentOptionItem,
                ),
                selectedPaymentOptionsItem = savedPaymentOptionItem,
                isEditing = true,
                isProcessing = false,
                canEdit = true,
                canRemove = true,
            )
        )
        val initialScreen = SelectSavedPaymentMethods(interactor)
        val viewModel = FakeBaseSheetViewModel.create(metadata, initialScreen)

        paparazziRule.snapshot {
            PaymentSheetScreen(viewModel = viewModel, type = PaymentSheetFlowType.Complete)
        }
    }

    @Test
    fun displaysSelectSavedPaymentMethodsInProcessingState() {
        val metadata = PaymentMethodMetadataFactory.create()
        val interactor = FakeSelectSavedPaymentMethodsInteractor(
            initialState = SelectSavedPaymentMethodsInteractor.State(
                paymentOptionsItems = listOf(
                    PaymentOptionsItem.AddCard,
                    savedPaymentOptionItem,
                ),
                selectedPaymentOptionsItem = savedPaymentOptionItem,
                isEditing = false,
                isProcessing = true,
                canEdit = false,
                canRemove = false,
            )
        )
        val initialScreen = SelectSavedPaymentMethods(interactor)
        val viewModel = FakeBaseSheetViewModel.create(metadata, initialScreen)

        paparazziRule.snapshot {
            PaymentSheetScreen(viewModel = viewModel, type = PaymentSheetFlowType.Complete)
        }
    }

    @Test
    fun displaysSelectSavedPaymentMethodsWithDefault() {
        val metadata = PaymentMethodMetadataFactory.create()
        val interactor = FakeSelectSavedPaymentMethodsInteractor(
            initialState = SelectSavedPaymentMethodsInteractor.State(
                paymentOptionsItems = listOf(
                    PaymentOptionsItem.AddCard,
                    defaultSavedPaymentOptionItem,
                    savedPaymentOptionItem,
                ),
                selectedPaymentOptionsItem = savedPaymentOptionItem,
                isEditing = false,
                isProcessing = false,
                canEdit = true,
                canRemove = true,
            )
        )
        val initialScreen = SelectSavedPaymentMethods(interactor)
        val viewModel = FakeBaseSheetViewModel.create(metadata, initialScreen)
        viewModel.primaryButtonUiStateSource.update { original ->
            original?.copy(enabled = true)
        }

        paparazziRule.snapshot {
            PaymentSheetScreen(viewModel = viewModel, type = PaymentSheetFlowType.Complete)
        }
    }

    @Test
    fun displaysSelectSavedPaymentMethodsInEditModeWithDefault() {
        val metadata = PaymentMethodMetadataFactory.create()
        val interactor = FakeSelectSavedPaymentMethodsInteractor(
            initialState = SelectSavedPaymentMethodsInteractor.State(
                paymentOptionsItems = listOf(
                    PaymentOptionsItem.AddCard,
                    defaultSavedPaymentOptionItem,
                    savedPaymentOptionItem,
                ),
                selectedPaymentOptionsItem = savedPaymentOptionItem,
                isEditing = true,
                isProcessing = false,
                canEdit = true,
                canRemove = true,
            )
        )
        val initialScreen = SelectSavedPaymentMethods(interactor)
        val viewModel = FakeBaseSheetViewModel.create(metadata, initialScreen)

        paparazziRule.snapshot {
            PaymentSheetScreen(viewModel = viewModel, type = PaymentSheetFlowType.Complete)
        }
    }
}
