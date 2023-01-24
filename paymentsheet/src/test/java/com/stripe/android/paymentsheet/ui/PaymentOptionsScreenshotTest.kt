package com.stripe.android.paymentsheet.ui

import com.stripe.android.model.PaymentMethodFixtures
import com.stripe.android.paymentsheet.PaymentOptionsItem
import com.stripe.android.paymentsheet.PaymentOptionsState
import com.stripe.android.utils.screenshots.FontSize
import com.stripe.android.utils.screenshots.PaparazziRule
import com.stripe.android.utils.screenshots.PaymentSheetAppearance
import com.stripe.android.utils.screenshots.SystemAppearance
import org.junit.Rule
import org.junit.Test

class PaymentOptionsScreenshotTest {

    @get:Rule
    val paparazziRule = PaparazziRule(
        SystemAppearance.values(),
        PaymentSheetAppearance.values(),
        FontSize.values(),
    )

    @Test
    fun testDefaultState() {
        paparazziRule.snapshot {
            PaymentOptions(
                state = PaymentOptionsState(
                    items = listOf(PaymentOptionsItem.AddCard, PaymentOptionsItem.GooglePay),
                    selectedIndex = 1,
                ),
                nameProvider = { it },
                isEditing = false,
                isProcessing = false,
                onAddCardPressed = {},
                onItemSelected = {},
                onItemRemoved = {},
            )
        }
    }

    @Test
    fun testEditingState() {
        val paymentMethod = PaymentMethodFixtures.CARD_PAYMENT_METHOD

        paparazziRule.snapshot {
            PaymentOptions(
                state = PaymentOptionsState(
                    items = listOf(
                        PaymentOptionsItem.AddCard,
                        PaymentOptionsItem.GooglePay,
                        PaymentOptionsItem.SavedPaymentMethod(
                            displayName = "Visa",
                            paymentMethod = paymentMethod,
                        )
                    ),
                    selectedIndex = 1,
                ),
                nameProvider = { it },
                isEditing = true,
                isProcessing = false,
                onAddCardPressed = {},
                onItemSelected = {},
                onItemRemoved = {},
            )
        }
    }

    @Test
    fun testProcessingState() {
        val paymentMethod = PaymentMethodFixtures.CARD_PAYMENT_METHOD

        paparazziRule.snapshot {
            PaymentOptions(
                state = PaymentOptionsState(
                    items = listOf(
                        PaymentOptionsItem.AddCard,
                        PaymentOptionsItem.GooglePay,
                        PaymentOptionsItem.SavedPaymentMethod(
                            displayName = "Visa",
                            paymentMethod = paymentMethod,
                        )
                    ),
                    selectedIndex = 1,
                ),
                nameProvider = { it },
                isEditing = false,
                isProcessing = true,
                onAddCardPressed = {},
                onItemSelected = {},
                onItemRemoved = {},
            )
        }
    }
}
