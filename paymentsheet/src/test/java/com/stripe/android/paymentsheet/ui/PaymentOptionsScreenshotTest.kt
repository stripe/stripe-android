package com.stripe.android.paymentsheet.ui

import androidx.compose.foundation.lazy.LazyListState
import com.stripe.android.core.strings.resolvableString
import com.stripe.android.core.utils.FeatureFlags
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.PaymentMethodFixtures
import com.stripe.android.paymentsheet.DisplayableSavedPaymentMethod
import com.stripe.android.paymentsheet.PaymentOptionsItem
import com.stripe.android.screenshottesting.PaparazziRule
import com.stripe.android.testing.FeatureFlagTestRule
import org.junit.Rule
import org.junit.Test

class PaymentOptionsScreenshotTest {

    @get:Rule
    val featureFlagTestRule = FeatureFlagTestRule(
        featureFlag = FeatureFlags.useNewUpdateCardScreen,
        isEnabled = false
    )

    @get:Rule
    val paparazziRule = PaparazziRule()

    @Test
    fun testWidthLessThanScreen() {
        featureFlagTestRule.setEnabled(false)
        paparazziRule.snapshot {
            SavedPaymentMethodTabLayoutUI(
                paymentOptionsItems = listOf(
                    PaymentOptionsItem.AddCard,
                    PaymentOptionsItem.Link,
                ),
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
        featureFlagTestRule.setEnabled(false)
        val paymentOptionsItems = listOf(
            PaymentOptionsItem.AddCard,
            PaymentOptionsItem.GooglePay,
            PaymentOptionsItem.SavedPaymentMethod(
                DisplayableSavedPaymentMethod.create(
                    displayName = "Card".resolvableString,
                    paymentMethod = createCard("4242"),
                ),
                canRemovePaymentMethods = true,
            ),
            PaymentOptionsItem.SavedPaymentMethod(
                DisplayableSavedPaymentMethod.create(
                    displayName = "Card".resolvableString,
                    paymentMethod = createCard("4000"),
                ),
                canRemovePaymentMethods = true,
            ),
            PaymentOptionsItem.SavedPaymentMethod(
                DisplayableSavedPaymentMethod.create(
                    displayName = "Card".resolvableString,
                    paymentMethod = createCard("1234"),
                ),
                canRemovePaymentMethods = true,
            ),
        )

        paparazziRule.snapshot {
            SavedPaymentMethodTabLayoutUI(
                paymentOptionsItems = paymentOptionsItems,
                selectedPaymentOptionsItem = paymentOptionsItems[1],
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
        featureFlagTestRule.setEnabled(false)
        val paymentOptionsItems = listOf(
            PaymentOptionsItem.AddCard,
            PaymentOptionsItem.GooglePay,
            PaymentOptionsItem.SavedPaymentMethod(
                DisplayableSavedPaymentMethod.create(
                    displayName = "Card".resolvableString,
                    paymentMethod = createCard("4242"),
                ),
                canRemovePaymentMethods = true,
            ),
            PaymentOptionsItem.SavedPaymentMethod(
                DisplayableSavedPaymentMethod.create(
                    displayName = "Card".resolvableString,
                    paymentMethod = createCard("4000"),
                ),
                canRemovePaymentMethods = true,
            ),
            PaymentOptionsItem.SavedPaymentMethod(
                DisplayableSavedPaymentMethod.create(
                    displayName = "Card".resolvableString,
                    paymentMethod = createCard("1234"),
                ),
                canRemovePaymentMethods = true,
            ),
        )

        paparazziRule.snapshot {
            SavedPaymentMethodTabLayoutUI(
                paymentOptionsItems = paymentOptionsItems,
                selectedPaymentOptionsItem = paymentOptionsItems[1],
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
        featureFlagTestRule.setEnabled(false)
        paparazziRule.snapshot {
            SavedPaymentMethodTabLayoutUI(
                paymentOptionsItems = paymentOptionsItemsWithRemoveDisabledAndModifiableCard,
                selectedPaymentOptionsItem = null,
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

    @Test
    fun testEditingAndRemoveDisabledWithModifiableItems_usingUpdatePaymentMethodScreen() {
        featureFlagTestRule.setEnabled(true)
        paparazziRule.snapshot {
            SavedPaymentMethodTabLayoutUI(
                paymentOptionsItems = paymentOptionsItemsWithRemoveDisabledAndModifiableCard,
                selectedPaymentOptionsItem = null,
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

    private val paymentOptionsItemsWithRemoveDisabledAndModifiableCard = listOf(
        PaymentOptionsItem.SavedPaymentMethod(
            DisplayableSavedPaymentMethod.create(
                displayName = "Card".resolvableString,
                paymentMethod = createCard("4242"),
            ),
            canRemovePaymentMethods = false,
        ),
        PaymentOptionsItem.SavedPaymentMethod(
            DisplayableSavedPaymentMethod.create(
                displayName = "Card".resolvableString,
                paymentMethod = createCard("4000"),
            ),
            canRemovePaymentMethods = false,
        ),
        PaymentOptionsItem.SavedPaymentMethod(
            DisplayableSavedPaymentMethod.create(
                displayName = "Card".resolvableString,
                paymentMethod = createCard("1234", addNetworks = true),
                isCbcEligible = true,
            ),
            canRemovePaymentMethods = false,
        ),
    )

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
