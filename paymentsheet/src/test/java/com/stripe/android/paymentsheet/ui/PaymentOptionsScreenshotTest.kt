package com.stripe.android.paymentsheet.ui

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
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

    private fun paymentOptionsScreenshotTestHelper(
        paymentOptionsItems: List<PaymentOptionsItem>,
        selectedPaymentOptionsItem: PaymentOptionsItem?,
        isEditing: Boolean,
        scrollState: LazyListState? = null,
    ) {
        paparazziRule.snapshot {
            SavedPaymentMethodTabLayoutUI(
                paymentOptionsItems = paymentOptionsItems,
                selectedPaymentOptionsItem = selectedPaymentOptionsItem,
                isEditing = isEditing,
                isProcessing = false,
                onAddCardPressed = {},
                onItemSelected = {},
                onModifyItem = {},
                scrollState = scrollState ?: rememberLazyListState()
            )
        }
    }

    @Test
    fun testWidthLessThanScreen() {
        paymentOptionsScreenshotTestHelper(
            paymentOptionsItems = listOf(
                PaymentOptionsItem.AddCard,
                PaymentOptionsItem.Link,
            ),
            selectedPaymentOptionsItem = PaymentOptionsItem.Link,
            isEditing = false,
        )
    }

    @Test
    fun testWidthMoreThanScreen() {
        val paymentOptionsItems = listOf(
            PaymentOptionsItem.AddCard,
            PaymentOptionsItem.GooglePay,
            PaymentOptionsItem.SavedPaymentMethod(
                DisplayableSavedPaymentMethod.create(
                    displayName = "Card".resolvableString,
                    paymentMethod = createCard("4242"),
                ),
            ),
            PaymentOptionsItem.SavedPaymentMethod(
                DisplayableSavedPaymentMethod.create(
                    displayName = "Card".resolvableString,
                    paymentMethod = createCard("4000"),
                ),
            ),
            PaymentOptionsItem.SavedPaymentMethod(
                DisplayableSavedPaymentMethod.create(
                    displayName = "Card".resolvableString,
                    paymentMethod = createCard("1234"),
                ),
            ),
        )

        paymentOptionsScreenshotTestHelper(
            paymentOptionsItems = paymentOptionsItems,
            selectedPaymentOptionsItem = paymentOptionsItems[1],
            isEditing = false,
        )
    }

    @Test
    fun testWidthMoreThanScreenAndScrollToEnd() {
        val paymentOptionsItems = listOf(
            PaymentOptionsItem.AddCard,
            PaymentOptionsItem.GooglePay,
            PaymentOptionsItem.SavedPaymentMethod(
                DisplayableSavedPaymentMethod.create(
                    displayName = "Card".resolvableString,
                    paymentMethod = createCard("4242"),
                ),
            ),
            PaymentOptionsItem.SavedPaymentMethod(
                DisplayableSavedPaymentMethod.create(
                    displayName = "Card".resolvableString,
                    paymentMethod = createCard("4000"),
                ),
            ),
            PaymentOptionsItem.SavedPaymentMethod(
                DisplayableSavedPaymentMethod.create(
                    displayName = "Card".resolvableString,
                    paymentMethod = createCard("1234"),
                ),
            ),
        )

        paymentOptionsScreenshotTestHelper(
            paymentOptionsItems = paymentOptionsItems,
            selectedPaymentOptionsItem = paymentOptionsItems[1],
            isEditing = false,
            scrollState = LazyListState(firstVisibleItemIndex = 2),
        )
    }

    @Test
    fun testEditingAndRemoveDisabledWithModifiableItems() {
        val paymentOptionsItems = listOf(
            PaymentOptionsItem.SavedPaymentMethod(
                DisplayableSavedPaymentMethod.create(
                    displayName = "Card".resolvableString,
                    paymentMethod = createCard("4242"),
                ),
            ),
            PaymentOptionsItem.SavedPaymentMethod(
                DisplayableSavedPaymentMethod.create(
                    displayName = "Card".resolvableString,
                    paymentMethod = createCard("4000"),
                ),
            ),
            PaymentOptionsItem.SavedPaymentMethod(
                DisplayableSavedPaymentMethod.create(
                    displayName = "Card".resolvableString,
                    paymentMethod = createCard("1234", addNetworks = true),
                    isCbcEligible = true,
                ),
            ),
        )

        paymentOptionsScreenshotTestHelper(
            paymentOptionsItems = paymentOptionsItems,
            selectedPaymentOptionsItem = null,
            isEditing = true,
            scrollState = LazyListState(firstVisibleItemIndex = 2),
        )
    }

    @Test
    fun testDefaultPaymentOptionEditing() {
        val paymentOptionsItemsWithDefaultCard = listOf(
            PaymentOptionsItem.SavedPaymentMethod(
                DisplayableSavedPaymentMethod.create(
                    displayName = "Card".resolvableString,
                    paymentMethod = createCard("8431"),
                    shouldShowDefaultBadge = true
                ),
            ),
            PaymentOptionsItem.SavedPaymentMethod(
                DisplayableSavedPaymentMethod.create(
                    displayName = "Card".resolvableString,
                    paymentMethod = createCard("4000"),
                ),
            ),
            PaymentOptionsItem.SavedPaymentMethod(
                DisplayableSavedPaymentMethod.create(
                    displayName = "Card".resolvableString,
                    paymentMethod = createCard("1234", addNetworks = true),
                    isCbcEligible = true,
                ),
            ),
        )

        paymentOptionsScreenshotTestHelper(
            paymentOptionsItems = paymentOptionsItemsWithDefaultCard,
            selectedPaymentOptionsItem = null,
            isEditing = true,
            scrollState = LazyListState(firstVisibleItemIndex = 2),
        )
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
