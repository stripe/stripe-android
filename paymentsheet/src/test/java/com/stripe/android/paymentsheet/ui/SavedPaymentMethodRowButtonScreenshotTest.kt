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
    private val savedAmex = createSavedCard(CardBrand.AmericanExpress, "0005")
    private val savedMastercard = createSavedCard(CardBrand.MasterCard, "4444")
    private val savedDiscover = createSavedCard(CardBrand.Discover, "1117")
    private val savedDiners = createSavedCard(CardBrand.DinersClub, "0004")
    private val savedJcb = createSavedCard(CardBrand.JCB, "0505")
    private val savedUnionPay = createSavedCard(CardBrand.UnionPay, "0005")
    private val savedCartesBincaires = createSavedCard(CardBrand.CartesBancaires, "1001")
    private val savedUnknownCard = createSavedCard(CardBrand.Unknown, "1234")
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
    fun testSavedAmex() {
        paparazziRule.snapshot {
            SavedPaymentMethodRowButton(
                displayableSavedPaymentMethod = savedAmex,
                resources = null,
                isEnabled = true,
                isSelected = false,
            )
        }
    }

    @Test
    fun testSavedMastercard() {
        paparazziRule.snapshot {
            SavedPaymentMethodRowButton(
                displayableSavedPaymentMethod = savedMastercard,
                resources = null,
                isEnabled = true,
                isSelected = false,
            )
        }
    }

    @Test
    fun testSavedDiscover() {
        paparazziRule.snapshot {
            SavedPaymentMethodRowButton(
                displayableSavedPaymentMethod = savedDiscover,
                resources = null,
                isEnabled = true,
                isSelected = false,
            )
        }
    }

    @Test
    fun testSavedDiners() {
        paparazziRule.snapshot {
            SavedPaymentMethodRowButton(
                displayableSavedPaymentMethod = savedDiners,
                resources = null,
                isEnabled = true,
                isSelected = false,
            )
        }
    }

    @Test
    fun testSavedJcb() {
        paparazziRule.snapshot {
            SavedPaymentMethodRowButton(
                displayableSavedPaymentMethod = savedJcb,
                resources = null,
                isEnabled = true,
                isSelected = false,
            )
        }
    }

    @Test
    fun testSavedUnionPay() {
        paparazziRule.snapshot {
            SavedPaymentMethodRowButton(
                displayableSavedPaymentMethod = savedUnionPay,
                resources = null,
                isEnabled = true,
                isSelected = false,
            )
        }
    }

    @Test
    fun testSavedCartesBincaires() {
        paparazziRule.snapshot {
            SavedPaymentMethodRowButton(
                displayableSavedPaymentMethod = savedCartesBincaires,
                resources = null,
                isEnabled = true,
                isSelected = false,
            )
        }
    }

    @Test
    fun testSavedUnknownCard() {
        paparazziRule.snapshot {
            SavedPaymentMethodRowButton(
                displayableSavedPaymentMethod = savedUnknownCard,
                resources = null,
                isEnabled = true,
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