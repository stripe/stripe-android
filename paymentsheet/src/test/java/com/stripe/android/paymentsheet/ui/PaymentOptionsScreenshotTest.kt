package com.stripe.android.paymentsheet.ui

import androidx.compose.foundation.lazy.LazyListState
import com.stripe.android.link.ui.LinkUi
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.PaymentMethodFixtures
import com.stripe.android.paymentsheet.PaymentOptionsItem
import com.stripe.android.paymentsheet.PaymentOptionsState
import com.stripe.android.screenshottesting.FontSize
import com.stripe.android.screenshottesting.PaparazziRule
import com.stripe.android.screenshottesting.SystemAppearance
import com.stripe.android.utils.screenshots.PaymentSheetAppearance
import org.junit.Rule
import org.junit.Test

class PaymentOptionsScreenshotTest {

    @get:Rule
    val paparazziRule = PaparazziRule(
        listOf(SystemAppearance.LightTheme),
        listOf(PaymentSheetAppearance.DefaultAppearance),
        listOf(FontSize.DefaultFont),
    )

    @Test
    fun testWidthLessThanScreen() {
        paparazziRule.snapshot {
            PaymentOptions(
                state = PaymentOptionsState(
                    items = listOf(
                        PaymentOptionsItem.AddCard,
                        PaymentOptionsItem.Link,
                    ),
                    selectedIndex = 1,
                ),
                isEditing = false,
                isProcessing = false,
                onAddCardPressed = {},
                onItemSelected = {},
                onModifyItem = {},
                onItemRemoved = {},
            )
        }
    }

    @Test
    fun testWithLinkRebrandDisabled() {
        LinkUi.useNewBrand = false
        paparazziRule.snapshot {
            PaymentOptions(
                state = PaymentOptionsState(
                    items = listOf(
                        PaymentOptionsItem.AddCard,
                        PaymentOptionsItem.Link,
                    ),
                    selectedIndex = 1,
                ),
                isEditing = false,
                isProcessing = false,
                onAddCardPressed = {},
                onItemSelected = {},
                onModifyItem = {},
                onItemRemoved = {},
            )
        }
        LinkUi.useNewBrand = true
    }

    @Test
    fun testWidthMoreThanScreen() {
        paparazziRule.snapshot {
            PaymentOptions(
                state = PaymentOptionsState(
                    items = listOf(
                        PaymentOptionsItem.AddCard,
                        PaymentOptionsItem.GooglePay,
                        PaymentOptionsItem.SavedPaymentMethod(
                            displayName = "Card",
                            paymentMethod = createCard("4242"),
                        ),
                        PaymentOptionsItem.SavedPaymentMethod(
                            displayName = "Card",
                            paymentMethod = createCard("4000"),
                        ),
                        PaymentOptionsItem.SavedPaymentMethod(
                            displayName = "Card",
                            paymentMethod = createCard("1234"),
                        ),
                    ),
                    selectedIndex = 1,
                ),
                isEditing = false,
                isProcessing = false,
                onAddCardPressed = {},
                onItemSelected = {},
                onModifyItem = {},
                onItemRemoved = {},
            )
        }
    }

    @Test
    fun testWidthMoreThanScreenAndScrollToEnd() {
        paparazziRule.snapshot {
            PaymentOptions(
                state = PaymentOptionsState(
                    items = listOf(
                        PaymentOptionsItem.AddCard,
                        PaymentOptionsItem.GooglePay,
                        PaymentOptionsItem.SavedPaymentMethod(
                            displayName = "Card",
                            paymentMethod = createCard("4242"),
                        ),
                        PaymentOptionsItem.SavedPaymentMethod(
                            displayName = "Card",
                            paymentMethod = createCard("4000"),
                        ),
                        PaymentOptionsItem.SavedPaymentMethod(
                            displayName = "Card",
                            paymentMethod = createCard("1234"),
                        ),
                    ),
                    selectedIndex = 1,
                ),
                isEditing = false,
                isProcessing = false,
                onAddCardPressed = {},
                onItemSelected = {},
                onModifyItem = {},
                onItemRemoved = {},
                scrollState = LazyListState(firstVisibleItemIndex = 2),
            )
        }
    }

    private fun createCard(last4: String): PaymentMethod {
        val original = PaymentMethodFixtures.createCard()
        return original.copy(
            card = original.card?.copy(last4 = last4),
        )
    }
}
