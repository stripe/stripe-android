package com.stripe.android.paymentsheet.ui

import androidx.compose.runtime.Composable
import com.stripe.android.core.strings.resolvableString
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadataFactory
import com.stripe.android.model.CardBrand
import com.stripe.android.model.PaymentMethodFixtures
import com.stripe.android.model.PaymentMethodFixtures.toDisplayableSavedPaymentMethod
import com.stripe.android.paymentsheet.DisplayableSavedPaymentMethod
import com.stripe.android.paymentsheet.viewmodels.FakeBaseSheetViewModel
import com.stripe.android.screenshottesting.PaparazziRule
import com.stripe.android.testing.PaymentMethodFactory
import org.junit.Rule
import org.junit.Test

internal class PaymentSheetScreenUpdatePaymentMethodScreenshotTest {
    @get:Rule
    val paparazziRule = PaparazziRule()

    @Test
    fun updatePaymentMethodScreen_forCard() {
        paparazziRule.snapshot {
            PaymentSheetScreenOnUpdatePaymentMethod(
                paymentMethod = PaymentMethodFactory.visaCard().toDisplayableSavedPaymentMethod(),
                canRemove = false
            )
        }
    }

    @Test
    fun updatePaymentMethodScreen_forCbcEligibleCard() {
        paparazziRule.snapshot {
            PaymentSheetScreenOnUpdatePaymentMethod(
                paymentMethod = PaymentMethodFixtures
                    .CARD_WITH_NETWORKS_PAYMENT_METHOD
                    .toDisplayableSavedPaymentMethod(),
                canRemove = true,
                initialCardBrand = CardBrand.CartesBancaires,
            )
        }
    }

    @Test
    fun updatePaymentMethodScreen_forCard_withRemoveButton() {
        paparazziRule.snapshot {
            PaymentSheetScreenOnUpdatePaymentMethod(
                paymentMethod = PaymentMethodFactory.visaCard().toDisplayableSavedPaymentMethod(),
                canRemove = true
            )
        }
    }

    @Test
    fun updatePaymentMethodScreen_forCard_withRemoveButton_withError() {
        paparazziRule.snapshot {
            PaymentSheetScreenOnUpdatePaymentMethod(
                paymentMethod = PaymentMethodFactory.visaCard().toDisplayableSavedPaymentMethod(),
                canRemove = true,
                error = "Something went wrong",
            )
        }
    }

    @Test
    fun updatePaymentMethodScreen_forUsBankAccount_withRemoveButton() {
        paparazziRule.snapshot {
            PaymentSheetScreenOnUpdatePaymentMethod(
                paymentMethod = PaymentMethodFixtures.US_BANK_ACCOUNT.toDisplayableSavedPaymentMethod(),
                canRemove = true
            )
        }
    }

    @Test
    fun updatePaymentMethodScreen_forSepaDebit_withRemoveButton() {
        paparazziRule.snapshot {
            PaymentSheetScreenOnUpdatePaymentMethod(
                paymentMethod = PaymentMethodFixtures.SEPA_DEBIT_PAYMENT_METHOD.toDisplayableSavedPaymentMethod(),
                canRemove = true
            )
        }
    }

    @Composable
    fun PaymentSheetScreenOnUpdatePaymentMethod(
        paymentMethod: DisplayableSavedPaymentMethod,
        canRemove: Boolean,
        initialCardBrand: CardBrand = CardBrand.Unknown,
        error: String? = null,
    ) {
        val interactor = FakeUpdatePaymentMethodInteractor(
            displayableSavedPaymentMethod = paymentMethod,
            canRemove = canRemove,
            viewActionRecorder = null,
            initialState = UpdatePaymentMethodInteractor.State(
                error = error?.resolvableString,
                isRemoving = false,
                cardBrandChoice = CardBrandChoice(brand = initialCardBrand)
            ),
        )
        val screen = com.stripe.android.paymentsheet.navigation.PaymentSheetScreen.UpdatePaymentMethod(interactor)
        val metadata = PaymentMethodMetadataFactory.create()
        val viewModel = FakeBaseSheetViewModel.create(metadata, screen)

        PaymentSheetScreen(viewModel = viewModel, type = PaymentSheetFlowType.Complete)
    }
}
