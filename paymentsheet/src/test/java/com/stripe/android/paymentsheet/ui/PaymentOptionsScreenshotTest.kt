package com.stripe.android.paymentsheet.ui

import androidx.compose.foundation.lazy.LazyListState
import com.stripe.android.core.strings.resolvableString
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.PaymentMethodFixtures
import com.stripe.android.paymentsheet.DisplayableSavedPaymentMethod
import com.stripe.android.paymentsheet.PaymentOptionsItem
import com.stripe.android.screenshottesting.PaparazziRule
import org.junit.Rule
import org.junit.Test

class PaymentOptionsScreenshotTest {

    @get:Rule
    val paparazziRule = PaparazziRule()

    @Test
    fun testWidthLessThanScreen() {
        paparazziRule.snapshot {
            SavedPaymentMethodTabLayoutUI(
                paymentOptionsItems = listOf(
                    PaymentOptionsItem.AddCard,
                    PaymentOptionsItem.Link,
                ),
                canRemoveSavedPaymentMethods = true,
                selectedPaymentOptionsItem = PaymentOptionsItem.Link,
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
    fun testWidthMoreThanScreen() {
        val paymentOptionsItems = listOf(
            PaymentOptionsItem.AddCard,
            PaymentOptionsItem.GooglePay,
            PaymentOptionsItem.SavedPaymentMethod(
                DisplayableSavedPaymentMethod(
                    displayName = "Card".resolvableString,
                    paymentMethod = createCard("4242"),
                )
            ),
            PaymentOptionsItem.SavedPaymentMethod(
                DisplayableSavedPaymentMethod(
                    displayName = "Card".resolvableString,
                    paymentMethod = createCard("4000"),
                )
            ),
            PaymentOptionsItem.SavedPaymentMethod(
                DisplayableSavedPaymentMethod(
                    displayName = "Card".resolvableString,
                    paymentMethod = createCard("1234"),
                )
            ),
        )

        paparazziRule.snapshot {
            SavedPaymentMethodTabLayoutUI(
                paymentOptionsItems = paymentOptionsItems,
                selectedPaymentOptionsItem = paymentOptionsItems[1],
                canRemoveSavedPaymentMethods = true,
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
        val paymentOptionsItems = listOf(
            PaymentOptionsItem.AddCard,
            PaymentOptionsItem.GooglePay,
            PaymentOptionsItem.SavedPaymentMethod(
                DisplayableSavedPaymentMethod(
                    displayName = "Card".resolvableString,
                    paymentMethod = createCard("4242"),
                )
            ),
            PaymentOptionsItem.SavedPaymentMethod(
                DisplayableSavedPaymentMethod(
                    displayName = "Card".resolvableString,
                    paymentMethod = createCard("4000"),
                )
            ),
            PaymentOptionsItem.SavedPaymentMethod(
                DisplayableSavedPaymentMethod(
                    displayName = "Card".resolvableString,
                    paymentMethod = createCard("1234"),
                )
            ),
        )

        paparazziRule.snapshot {
            SavedPaymentMethodTabLayoutUI(
                paymentOptionsItems = paymentOptionsItems,
                selectedPaymentOptionsItem = paymentOptionsItems[1],
                canRemoveSavedPaymentMethods = true,
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

    @Test
    fun testEditingAndRemoveDisabledWithModifiableItems() {
        val paymentOptionsItems = listOf(
            PaymentOptionsItem.SavedPaymentMethod(
                DisplayableSavedPaymentMethod(
                    displayName = "Card".resolvableString,
                    paymentMethod = createCard("4242"),
                )
            ),
            PaymentOptionsItem.SavedPaymentMethod(
                DisplayableSavedPaymentMethod(
                    displayName = "Card".resolvableString,
                    paymentMethod = createCard("4000"),
                )
            ),
            PaymentOptionsItem.SavedPaymentMethod(
                DisplayableSavedPaymentMethod(
                    displayName = "Card".resolvableString,
                    paymentMethod = createCard("1234", addNetworks = true),
                    isCbcEligible = true,
                )
            ),
        )

        paparazziRule.snapshot {
            SavedPaymentMethodTabLayoutUI(
                paymentOptionsItems = paymentOptionsItems,
                selectedPaymentOptionsItem = null,
                canRemoveSavedPaymentMethods = false,
                isEditing = true,
                isProcessing = false,
                onAddCardPressed = {},
                onItemSelected = {},
                onModifyItem = {},
                onItemRemoved = {},
                scrollState = LazyListState(firstVisibleItemIndex = 2),
            )
        }
    }

    private fun createCard(last4: String, addNetworks: Boolean = false): PaymentMethod {
        val original = PaymentMethodFixtures.createCard()
        return original.copy(
            card = original.card?.copy(
                last4 = last4,
                networks = if (addNetworks) {
                    PaymentMethod.Card.Networks(
                        available = setOf("visa", "cartes_bancaires"),
                    )
                } else {
                    null
                }
            ),
        )
    }
}
