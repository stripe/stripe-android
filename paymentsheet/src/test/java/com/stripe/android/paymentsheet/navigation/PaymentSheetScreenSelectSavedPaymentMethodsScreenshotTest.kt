package com.stripe.android.paymentsheet.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadataFactory
import com.stripe.android.model.PaymentMethodFixtures
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
        displayableSavedPaymentMethod = PaymentMethodFixtures.displayableCard(),
    )

    private val defaultSavedPaymentOptionItem = PaymentOptionsItem.SavedPaymentMethod(
        displayableSavedPaymentMethod = PaymentMethodFixtures.defaultDisplayableCard(),
    )

    private fun getTestViewModel(
        includeDefaultPaymentMethod: Boolean,
        isEditing: Boolean,
        isProcessing: Boolean,
        canEdit: Boolean,
        canRemove: Boolean,
    ): FakeBaseSheetViewModel {
        val metadata = PaymentMethodMetadataFactory.create()
        val interactor = FakeSelectSavedPaymentMethodsInteractor(
            initialState = SelectSavedPaymentMethodsInteractor.State(
                paymentOptionsItems = if (includeDefaultPaymentMethod) {
                    listOf(
                        PaymentOptionsItem.AddCard,
                        defaultSavedPaymentOptionItem,
                        savedPaymentOptionItem,
                    )
                } else {
                    listOf(
                        PaymentOptionsItem.AddCard,
                        savedPaymentOptionItem,
                    )
                },
                selectedPaymentOptionsItem = savedPaymentOptionItem,
                isEditing = isEditing,
                isProcessing = isProcessing,
                canEdit = canEdit,
                canRemove = canRemove,
            )
        )
        val initialScreen = SelectSavedPaymentMethods(interactor)
        return FakeBaseSheetViewModel.create(metadata, initialScreen)
    }

    @Test
    fun displaysSelectSavedPaymentMethods() {
        val viewModel = getTestViewModel(
            includeDefaultPaymentMethod = false,
            isEditing = false,
            isProcessing = false,
            canEdit = true,
            canRemove = true,
        )
        viewModel.primaryButtonUiStateSource.update { original ->
            original?.copy(enabled = true)
        }

        paparazziRule.snapshot {
            PaymentSheetScreen(viewModel = viewModel, type = PaymentSheetFlowType.Complete)
        }
    }

    @Test
    fun displaysSelectSavedPaymentMethodsInEditMode() {
        val viewModel = getTestViewModel(
            includeDefaultPaymentMethod = false,
            isEditing = true,
            isProcessing = false,
            canEdit = true,
            canRemove = true,
        )

        paparazziRule.snapshot {
            PaymentSheetScreen(viewModel = viewModel, type = PaymentSheetFlowType.Complete)
        }
    }

    @Test
    fun displaysSelectSavedPaymentMethodsInProcessingState() {
        val viewModel = getTestViewModel(
            includeDefaultPaymentMethod = false,
            isEditing = false,
            isProcessing = true,
            canEdit = false,
            canRemove = false,
        )

        paparazziRule.snapshot {
            PaymentSheetScreen(viewModel = viewModel, type = PaymentSheetFlowType.Complete)
        }
    }

    @Test
    fun displaysSelectSavedPaymentMethodsWithDefault() {
        val viewModel = getTestViewModel(
            includeDefaultPaymentMethod = true,
            isEditing = true,
            isProcessing = false,
            canEdit = true,
            canRemove = true,
        )

        paparazziRule.snapshot {
            PaymentSheetScreen(viewModel = viewModel, type = PaymentSheetFlowType.Complete)
        }
    }
}
