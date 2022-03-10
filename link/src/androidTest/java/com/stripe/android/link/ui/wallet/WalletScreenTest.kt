package com.stripe.android.link.ui.wallet

import android.content.Intent
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.filter
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onChildren
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onParent
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import com.stripe.android.PaymentConfiguration
import com.stripe.android.link.LinkActivity
import com.stripe.android.link.LinkActivityContract
import com.stripe.android.link.createAndroidIntentComposeRule
import com.stripe.android.link.theme.DefaultLinkTheme
import com.stripe.android.model.CardBrand
import com.stripe.android.model.ConsumerPaymentDetails
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
internal class WalletScreenTest {
    @get:Rule
    val composeTestRule = createAndroidIntentComposeRule<LinkActivity> {
        PaymentConfiguration.init(it, "publishable_key")
        Intent(it, LinkActivity::class.java).apply {
            putExtra(
                LinkActivityContract.EXTRA_ARGS,
                LinkActivityContract.Args("Merchant, Inc")
            )
        }
    }

    private val payButtonLabel = "Pay $10.99"
    private val paymentDetails = listOf(
        ConsumerPaymentDetails.Card(
            "id1",
            true,
            2022,
            12,
            CardBrand.Visa,
            "4242"
        ),
        ConsumerPaymentDetails.Card(
            "id2",
            false,
            2023,
            11,
            CardBrand.MasterCard,
            "4444"
        )
    )

    @Test
    fun default_payment_method_is_initially_selected() {
        var paymentMethod: ConsumerPaymentDetails.PaymentDetails? = null
        setContent(
            onPayButtonClick = {
                paymentMethod = it
            }
        )

        onPayButton().performClick()
        assertThat(paymentMethod).isEqualTo(paymentDetails.first())
    }

    @Test
    fun selected_payment_method_is_shown_when_collapsed() {
        setContent()

        val secondPaymentMethod = paymentDetails[1]

        toggleListExpanded()
        select(secondPaymentMethod)
        toggleListExpanded()

        composeTestRule.onNodeWithText("Pay with").onParent().onChildren()
            .filter(hasText(secondPaymentMethod.last4, substring = true)).assertCountEquals(1)
    }

    @Test
    fun selected_payment_method_is_used_for_payment() {
        var paymentMethod: ConsumerPaymentDetails.PaymentDetails? = null
        setContent(
            onPayButtonClick = {
                paymentMethod = it
            }
        )

        val secondPaymentMethod = paymentDetails[1]

        toggleListExpanded()
        select(secondPaymentMethod)
        onPayButton().performClick()

        assertThat(paymentMethod).isEqualTo(secondPaymentMethod)
    }

    @Test
    fun add_new_payment_method_click_triggers_action() {
        var count = 0
        setContent(
            onAddNewPaymentMethodClick = {
                count++
            }
        )

        toggleListExpanded()
        composeTestRule.onNodeWithText("Add a new payment method").performClick()

        assertThat(count).isEqualTo(1)
    }

    @Test
    fun pay_another_way_click_triggers_action() {
        var count = 0
        setContent(
            onPayAnotherWayClick = {
                count++
            }
        )

        toggleListExpanded()
        composeTestRule.onNodeWithText("Pay another way").performClick()

        assertThat(count).isEqualTo(1)
    }

    private fun setContent(
        onAddNewPaymentMethodClick: () -> Unit = {},
        onPayButtonClick: (ConsumerPaymentDetails.PaymentDetails) -> Unit = {},
        onPayAnotherWayClick: () -> Unit = {}
    ) = composeTestRule.setContent {
        DefaultLinkTheme {
            WalletBody(
                paymentDetails = paymentDetails,
                payButtonLabel = payButtonLabel,
                onAddNewPaymentMethodClick = onAddNewPaymentMethodClick,
                onPayButtonClick = onPayButtonClick,
                onPayAnotherWayClick = onPayAnotherWayClick
            )
        }
    }

    private fun toggleListExpanded() =
        composeTestRule.onNodeWithTag("ChevronIcon", useUnmergedTree = true).performClick()

    private fun select(paymentDetails: ConsumerPaymentDetails.Card) =
        composeTestRule.onNodeWithText(paymentDetails.last4, substring = true).performClick()

    private fun onPayButton() = composeTestRule.onNodeWithText(payButtonLabel)
}
