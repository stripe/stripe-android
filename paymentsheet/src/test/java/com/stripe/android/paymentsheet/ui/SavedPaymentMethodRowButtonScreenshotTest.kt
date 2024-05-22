package com.stripe.android.paymentsheet.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.stripe.android.model.CardBrand
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.PaymentMethodFixtures
import com.stripe.android.paymentsheet.DisplayableSavedPaymentMethod
import com.stripe.android.screenshottesting.FontSize
import com.stripe.android.screenshottesting.PaparazziRule
import com.stripe.android.screenshottesting.SystemAppearance
import com.stripe.android.utils.screenshots.PaymentSheetAppearance
import org.junit.Rule
import kotlin.test.Test

internal class SavedPaymentMethodRowButtonScreenshotTest {

    @get:Rule
    val paparazziRule = PaparazziRule(
        SystemAppearance.entries,
        PaymentSheetAppearance.entries,
        FontSize.entries,
        boxModifier = Modifier
            .padding(16.dp)
    )

    private val savedVisa = createSavedCard(CardBrand.Visa, "4242")

    private val savedSepaDebit = DisplayableSavedPaymentMethod(
        displayName = "4242",
        paymentMethod = PaymentMethod(
            id = "002",
            created = null,
            liveMode = false,
            code = PaymentMethod.Type.SepaDebit.code,
            type = PaymentMethod.Type.SepaDebit,
        )
    )

    private val savedUsBankAccount =
        DisplayableSavedPaymentMethod(
            displayName = "4242",
            paymentMethod = PaymentMethod(
                id = "002",
                created = null,
                liveMode = false,
                code = PaymentMethod.Type.USBankAccount.code,
                type = PaymentMethod.Type.USBankAccount,
                usBankAccount = PaymentMethodFixtures.US_BANK_ACCOUNT.usBankAccount,
            )
        )

    private val savedOtherPaymentMethod = DisplayableSavedPaymentMethod(
        displayName = "Klarna",
        paymentMethod = PaymentMethod(
            id = "002",
            created = null,
            liveMode = false,
            code = PaymentMethod.Type.Klarna.code,
            type = PaymentMethod.Type.Klarna,
        )
    )

    @Test
    fun testSavedVisa() {
        paparazziRule.snapshot {
            SavedPaymentMethodRowButton(
                displayableSavedPaymentMethod = savedVisa,
                resources = null,
                isEnabled = true,
                isSelected = false,
            )
        }
    }

    @Test
    fun testSavedVisa_selected() {
        paparazziRule.snapshot {
            SavedPaymentMethodRowButton(
                displayableSavedPaymentMethod = savedVisa,
                resources = null,
                isEnabled = true,
                isSelected = true,
            )
        }
    }

    @Test
    fun testSavedVisa_disabled() {
        paparazziRule.snapshot {
            SavedPaymentMethodRowButton(
                displayableSavedPaymentMethod = savedVisa,
                resources = null,
                isEnabled = false,
                isSelected = false,
            )
        }
    }

    @Test
    fun testSavedUsBankAccount() {
        paparazziRule.snapshot {
            SavedPaymentMethodRowButton(
                displayableSavedPaymentMethod = savedUsBankAccount,
                resources = null,
                isEnabled = true,
                isSelected = false,
            )
        }
    }

    @Test
    fun testSavedUsBankAccount_selected() {
        paparazziRule.snapshot {
            SavedPaymentMethodRowButton(
                displayableSavedPaymentMethod = savedUsBankAccount,
                resources = null,
                isEnabled = true,
                isSelected = true,
            )
        }
    }

    @Test
    fun testSavedUsBankAccount_disabled() {
        paparazziRule.snapshot {
            SavedPaymentMethodRowButton(
                displayableSavedPaymentMethod = savedUsBankAccount,
                resources = null,
                isEnabled = false,
                isSelected = false,
            )
        }
    }

    @Test
    fun testSavedSepaDebit() {
        paparazziRule.snapshot {
            SavedPaymentMethodRowButton(
                displayableSavedPaymentMethod = savedSepaDebit,
                resources = null,
                isEnabled = true,
                isSelected = false,
            )
        }
    }

    @Test
    fun testSavedOtherPaymentMethod() {
        paparazziRule.snapshot {
            SavedPaymentMethodRowButton(
                displayableSavedPaymentMethod = savedOtherPaymentMethod,
                resources = null,
                isEnabled = true,
                isSelected = false,
            )
        }
    }

    private fun createSavedCard(brand: CardBrand, last4: String) =
        DisplayableSavedPaymentMethod(
            displayName = "····$last4",
            paymentMethod = PaymentMethod(
                id = "001",
                created = null,
                liveMode = false,
                code = PaymentMethod.Type.Card.code,
                type = PaymentMethod.Type.Card,
                card = PaymentMethod.Card(
                    brand = brand,
                    last4 = last4,
                )
            )
        )
}
