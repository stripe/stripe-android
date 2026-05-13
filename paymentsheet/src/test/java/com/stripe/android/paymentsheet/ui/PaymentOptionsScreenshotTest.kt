package com.stripe.android.paymentsheet.ui

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import com.stripe.android.core.strings.resolvableString
import com.stripe.android.model.CardBrand
import com.stripe.android.model.LinkBrand
import com.stripe.android.model.LinkPaymentDetails
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
        createSavedPaymentMethodTabLayoutUiScreenshot(
            paymentOptionsItems = listOf(
                PaymentOptionsItem.AddCard,
                PaymentOptionsItem.Link(LinkBrand.Link),
            ),
            selectedPaymentOptionsItem = PaymentOptionsItem.Link(LinkBrand.Link),
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

        createSavedPaymentMethodTabLayoutUiScreenshot(
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

        createSavedPaymentMethodTabLayoutUiScreenshot(
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

        createSavedPaymentMethodTabLayoutUiScreenshot(
            paymentOptionsItems = paymentOptionsItems,
            selectedPaymentOptionsItem = null,
            isEditing = true,
            scrollState = LazyListState(firstVisibleItemIndex = 2),
        )
    }

    @Test
    fun testDefaultPaymentOptionEditing() {
        createSavedPaymentMethodTabLayoutUiScreenshot(
            paymentOptionsItems = paymentOptionsItemsWithDefaultCard,
            selectedPaymentOptionsItem = null,
            isEditing = true,
            scrollState = LazyListState(firstVisibleItemIndex = 2),
        )
    }

    @Test
    fun testNotlink() {
        createSavedPaymentMethodTabLayoutUiScreenshot(
            paymentOptionsItems = listOf(
                PaymentOptionsItem.AddCard,
                PaymentOptionsItem.Link(LinkBrand.Notlink),
                PaymentOptionsItem.SavedPaymentMethod(
                    DisplayableSavedPaymentMethod.create(
                        displayName = "4242".resolvableString,
                        paymentMethod = PaymentMethod(
                            id = "004",
                            created = null,
                            liveMode = false,
                            code = PaymentMethod.Type.Link.code,
                            type = PaymentMethod.Type.Link,
                            linkPaymentDetails = LinkPaymentDetails.Card(
                                nickname = null,
                                expMonth = 1,
                                expYear = 2030,
                                last4 = "4242",
                                brand = CardBrand.Visa,
                                funding = "CREDIT",
                            ),
                        ),
                    ),
                )
            ),
            selectedPaymentOptionsItem = PaymentOptionsItem.Link(LinkBrand.Notlink),
            isEditing = false,
            linkBrand = LinkBrand.Notlink,
        )
    }

    private val paymentOptionsItemsWithDefaultCard = listOf(
        PaymentOptionsItem.SavedPaymentMethod(
            DisplayableSavedPaymentMethod.create(
                displayName = "Card".resolvableString,
                paymentMethod = createCard("8431"),
                shouldShowDefaultBadge = true,
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

    private fun createSavedPaymentMethodTabLayoutUiScreenshot(
        paymentOptionsItems: List<PaymentOptionsItem>,
        selectedPaymentOptionsItem: PaymentOptionsItem?,
        isEditing: Boolean,
        linkBrand: LinkBrand = LinkBrand.Link,
        scrollState: LazyListState? = null,
    ) {
        paparazziRule.snapshot {
            SavedPaymentMethodTabLayoutUI(
                paymentOptionsItems = paymentOptionsItems,
                selectedPaymentOptionsItem = selectedPaymentOptionsItem,
                linkBrand = linkBrand,
                isEditing = isEditing,
                isProcessing = false,
                onAddCardPressed = {},
                onItemSelected = {},
                onModifyItem = {},
                scrollState = scrollState ?: rememberLazyListState()
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
