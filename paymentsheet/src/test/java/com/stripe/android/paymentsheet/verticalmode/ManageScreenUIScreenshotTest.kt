package com.stripe.android.paymentsheet.verticalmode

import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.PaymentMethodFixtures
import com.stripe.android.paymentsheet.DisplayableSavedPaymentMethod
import com.stripe.android.screenshottesting.PaparazziRule
import org.junit.Rule
import org.junit.Test

internal class ManageScreenUIScreenshotTest {

    @get:Rule
    val paparazziRule = PaparazziRule(
        boxModifier = Modifier
            .padding(16.dp)
    )

    @Test
    fun testManageUIScreen_noSelectedPMs() {
        paparazziRule.snapshot {
            ManageScreenUI(
                interactor = FakeManageScreenInteractor(
                    initialState = ManageScreenInteractor.State(
                        paymentMethods = savedPaymentMethods,
                        currentSelection = null
                    )
                ),
            )
        }
    }

    @Test
    fun testManageUIScreen_withSelectedPM() {
        paparazziRule.snapshot {
            ManageScreenUI(
                interactor = FakeManageScreenInteractor(
                    initialState = ManageScreenInteractor.State(
                        paymentMethods = savedPaymentMethods,
                        currentSelection = savedPaymentMethods[1]
                    )
                ),
            )
        }
    }

    private val savedPaymentMethods: List<DisplayableSavedPaymentMethod> = listOf(
        createCard("4242"),
        createCard("4000"),
        createUsBank("1001"),
        createCard("1234"),
    ).map {
        DisplayableSavedPaymentMethod(
            displayName = it.card?.last4 ?: it.usBankAccount?.last4 ?: "",
            paymentMethod = it,
            isCbcEligible = false,
        )
    }

    private fun createCard(last4: String): PaymentMethod {
        val original = PaymentMethodFixtures.createCard()
        return original.copy(
            card = original.card?.copy(last4 = last4),
        )
    }

    private fun createUsBank(last4: String): PaymentMethod {
        val original = PaymentMethodFixtures.US_BANK_ACCOUNT
        return original.copy(usBankAccount = original.usBankAccount?.copy(last4 = last4))
    }
}
