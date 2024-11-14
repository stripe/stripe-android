package com.stripe.android.paymentsheet.ui

import androidx.compose.runtime.Composable
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadataFactory
import com.stripe.android.model.PaymentMethodFixtures
import com.stripe.android.model.PaymentMethodFixtures.toDisplayableSavedPaymentMethod
import com.stripe.android.paymentsheet.DisplayableSavedPaymentMethod
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
        val card = PaymentMethodFactory.visaCard()
        paparazziRule.snapshot {
            PaymentSheetScreenOnUpdatePaymentMethod(
                displayableSavedPaymentMethod = card.toDisplayableSavedPaymentMethod(),
                updateablePaymentMethod = UpdateablePaymentMethod.Card(card.card!!),
                canRemove = false
            )
        }
    }

    @Test
    fun updatePaymentMethodScreen_forCard_withRemoveButton() {
        val card = PaymentMethodFactory.visaCard()
        paparazziRule.snapshot {
            PaymentSheetScreenOnUpdatePaymentMethod(
                displayableSavedPaymentMethod = card.toDisplayableSavedPaymentMethod(),
                updateablePaymentMethod = UpdateablePaymentMethod.Card(card.card!!),
                canRemove = true
            )
        }
    }

    @Test
    fun updatePaymentMethodScreen_forUsBankAccount_withRemoveButton() {
        val usBankAccount = PaymentMethodFixtures.US_BANK_ACCOUNT
        paparazziRule.snapshot {
            PaymentSheetScreenOnUpdatePaymentMethod(
                displayableSavedPaymentMethod = usBankAccount.toDisplayableSavedPaymentMethod(),
                updateablePaymentMethod = UpdateablePaymentMethod.UsBankAccount(usBankAccount.usBankAccount!!),
                canRemove = true
            )
        }
    }

    @Test
    fun updatePaymentMethodScreen_forSepaDebit_withRemoveButton() {
        val sepaDebit = PaymentMethodFixtures.SEPA_DEBIT_PAYMENT_METHOD
        paparazziRule.snapshot {
            PaymentSheetScreenOnUpdatePaymentMethod(
                displayableSavedPaymentMethod = sepaDebit.toDisplayableSavedPaymentMethod(),
                updateablePaymentMethod = UpdateablePaymentMethod.SepaDebit(sepaDebit.sepaDebit!!),
                canRemove = true
            )
        }
    }

    @Composable
    internal fun PaymentSheetScreenOnUpdatePaymentMethod(
        canRemove: Boolean,
        displayableSavedPaymentMethod: DisplayableSavedPaymentMethod,
        updateablePaymentMethod: UpdateablePaymentMethod,
    ) {
        val interactor = DefaultUpdatePaymentMethodInteractor(
            isLiveMode = true,
            displayableSavedPaymentMethod = displayableSavedPaymentMethod,
            paymentMethod = updateablePaymentMethod,
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
