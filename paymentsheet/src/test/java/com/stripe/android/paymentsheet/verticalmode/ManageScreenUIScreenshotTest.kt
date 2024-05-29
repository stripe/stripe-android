package com.stripe.android.paymentsheet.verticalmode

import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.PaymentMethodFixtures
import com.stripe.android.paymentsheet.FakeManageScreenInteractor
import com.stripe.android.screenshottesting.FontSize
import com.stripe.android.screenshottesting.PaparazziRule
import com.stripe.android.screenshottesting.SystemAppearance
import com.stripe.android.utils.screenshots.PaymentSheetAppearance
import org.junit.Rule
import org.junit.Test

internal class ManageScreenUIScreenshotTest {

    @get:Rule
    val paparazziRule = PaparazziRule(
        SystemAppearance.entries,
        PaymentSheetAppearance.entries,
        FontSize.entries,
        boxModifier = Modifier
            .padding(16.dp)
    )

    @Test
    fun testManageUIScreen_noSelectedPMs() {
        paparazziRule.snapshot {
            ManageScreenUI(
                interactor = FakeManageScreenInteractor(paymentMethods = savedPaymentMethods),
            )
        }
    }

    private val savedPaymentMethods: List<PaymentMethod> = listOf(
        createCard("4242"),
        createCard("4000"),
        createUsBank("1001"),
        createCard("1234"),
    )

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
