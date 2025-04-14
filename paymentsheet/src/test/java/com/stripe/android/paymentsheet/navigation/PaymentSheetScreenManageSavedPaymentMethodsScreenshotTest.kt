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
        listOf(
            FIRST_CARD to FIRST_CARD.card!!.last4!!,
            US_BANK_ACCOUNT to US_BANK_ACCOUNT.usBankAccount!!.last4!!,
        ).map {
            DisplayableSavedPaymentMethod.create(
                displayName = it.second.resolvableString,
                paymentMethod = it.first,
                isCbcEligible = true
            )
        }

    private val displayableCards =
        listOf(
            FIRST_CARD to FIRST_CARD.card!!.last4!!,
            SECOND_CARD to SECOND_CARD.card!!.last4!!,
        ).map {
            DisplayableSavedPaymentMethod.create(
                displayName = it.second.resolvableString,
                paymentMethod = it.first,
                isCbcEligible = true
            )
        }

    @Test
    fun displaysSelectMode() {
        screenshotTest(
            paymentMethods = displayableSavedPaymentMethods,
            selection = displayableSavedPaymentMethods.first(),
            isEditing = false
        )
    }

    @Test
    fun displaysEditMode() {
        screenshotTest(paymentMethods = displayableSavedPaymentMethods, isEditing = true)
    }

    @Test
    fun displaysSelectModeWithCardsOnly() {
        screenshotTest(
            paymentMethods = displayableCards,
            selection = displayableCards.first(),
            isEditing = false
        )
    }

    @Test
    fun displaysEditModeWithCardsOnly() {
        screenshotTest(paymentMethods = displayableCards, isEditing = true)
    }

    private fun screenshotTest(
        paymentMethods: List<DisplayableSavedPaymentMethod>,
        selection: DisplayableSavedPaymentMethod? = null,
        isEditing: Boolean,
    ) {
        val metadata = PaymentMethodMetadataFactory.create()
        val interactor = FakeManageScreenInteractor(
            initialState = ManageScreenInteractor.State(
                paymentMethods = paymentMethods,
                currentSelection = selection,
                isEditing = isEditing,
                canEdit = true,
            )
        )
        val initialScreen = ManageSavedPaymentMethods(interactor)
        val viewModel = FakeBaseSheetViewModel.create(metadata, initialScreen, canGoBack = true)

        paparazziRule.snapshot {
            PaymentSheetScreen(viewModel = viewModel, type = PaymentSheetFlowType.Complete)
        }
    }

    private companion object {
        private val FIRST_CARD = PaymentMethodFixtures.CARD_PAYMENT_METHOD
        private val SECOND_CARD = PaymentMethodFixtures.CARD_WITH_NETWORKS_PAYMENT_METHOD
        private val US_BANK_ACCOUNT = PaymentMethodFixtures.US_BANK_ACCOUNT_VERIFIED
    }
}
