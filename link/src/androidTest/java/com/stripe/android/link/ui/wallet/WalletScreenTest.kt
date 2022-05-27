package com.stripe.android.link.ui.wallet

import android.content.Intent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.ModalBottomSheetLayout
import androidx.compose.material.ModalBottomSheetValue
import androidx.compose.material.rememberModalBottomSheetState
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.filter
import androidx.compose.ui.test.filterToOne
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onChildren
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onParent
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import com.stripe.android.PaymentConfiguration
import com.stripe.android.link.LinkActivity
import com.stripe.android.link.LinkActivityContract
import com.stripe.android.link.StripeIntentFixtures
import com.stripe.android.link.createAndroidIntentComposeRule
import com.stripe.android.link.theme.DefaultLinkTheme
import com.stripe.android.link.ui.BottomSheetContent
import com.stripe.android.link.ui.ErrorMessage
import com.stripe.android.model.CardBrand
import com.stripe.android.model.ConsumerPaymentDetails
import kotlinx.coroutines.launch
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@OptIn(ExperimentalMaterialApi::class)
@RunWith(AndroidJUnit4::class)
internal class WalletScreenTest {
    @get:Rule
    val composeTestRule = createAndroidIntentComposeRule<LinkActivity> {
        PaymentConfiguration.init(it, "publishable_key")
        Intent(it, LinkActivity::class.java).apply {
            putExtra(
                LinkActivityContract.EXTRA_ARGS,
                LinkActivityContract.Args(
                    StripeIntentFixtures.PI_SUCCEEDED,
                    true,
                    "Merchant, Inc"
                )
            )
        }
    }

    private val primaryButtonLabel = "Pay $10.99"
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

        onPrimaryButton().performClick()
        assertThat(paymentMethod).isEqualTo(paymentDetails.first())
    }

    @Test
    fun selected_payment_method_is_shown_when_collapsed() {
        setContent()

        val secondPaymentMethod = paymentDetails[1]

        toggleListExpanded()
        onPaymentDetailsItem(secondPaymentMethod).performClick()
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
        onPaymentDetailsItem(secondPaymentMethod).performClick()
        onPrimaryButton().performClick()

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

    @Test
    fun options_menu_sets_bottom_sheet_content() {
        var bottomSheetContent: BottomSheetContent? = null
        setContent(
            showBottomSheetContent = {
                bottomSheetContent = it
            }
        )

        val secondPaymentMethod = paymentDetails[1]

        toggleListExpanded()
        onOptionsForPaymentMethod(secondPaymentMethod).performClick()

        assertThat(bottomSheetContent).isNotNull()
    }

    @Test
    fun delete_item_shows_dialog_confirmation() {
        setContent()
        toggleListExpanded()
        onOptionsForPaymentMethod(paymentDetails.first()).performClick()
        onRemoveCardButton().assertExists()
        onRemoveCardButton().performClick()
        onRemoveConfirmationDialog().assertExists()
    }

    @Test
    fun canceling_bottom_sheet_dismisses_it() {
        setContent()
        toggleListExpanded()
        onOptionsForPaymentMethod(paymentDetails.first()).performClick()
        onCancelButton().performClick()
        onRemoveCardButton().assertDoesNotExist()
    }

    @Test
    fun confirming_delete_item_triggers_callback() {
        var paymentMethod: ConsumerPaymentDetails.PaymentDetails? = null
        setContent(
            onDeletePaymentMethod = {
                paymentMethod = it
            }
        )
        toggleListExpanded()
        onOptionsForPaymentMethod(paymentDetails.first()).performClick()
        onRemoveCardButton().performClick()
        onRemoveButton().performClick()
        assertThat(paymentMethod).isEqualTo(paymentDetails.first())
    }

    @Test
    fun canceling_delete_item_keeps_item() {
        var paymentMethod: ConsumerPaymentDetails.PaymentDetails? = null
        setContent(
            onDeletePaymentMethod = {
                paymentMethod = it
            }
        )
        toggleListExpanded()
        onOptionsForPaymentMethod(paymentDetails.first()).performClick()
        onRemoveCardButton().performClick()
        onCancelButton().performClick()
        assertThat(paymentMethod).isNull()
    }

    @Test
    fun canceling_delete_item_then_trying_again_shows_dialog() {
        setContent()
        toggleListExpanded()
        onOptionsForPaymentMethod(paymentDetails.first()).performClick()
        onRemoveCardButton().performClick()
        onCancelButton().performClick()
        onOptionsForPaymentMethod(paymentDetails.first()).performClick()
        onRemoveCardButton().performClick()
        onRemoveConfirmationDialog().assertExists()
    }

    @Test
    fun when_error_message_is_not_null_then_it_is_visible() {
        val errorMessage = "Error message"
        setContent(errorMessage = ErrorMessage.Raw(errorMessage))
        composeTestRule.onNodeWithText(errorMessage).assertExists()
    }

    private fun setContent(
        errorMessage: ErrorMessage? = null,
        onAddNewPaymentMethodClick: () -> Unit = {},
        onDeletePaymentMethod: (ConsumerPaymentDetails.PaymentDetails) -> Unit = {},
        onPayButtonClick: (ConsumerPaymentDetails.PaymentDetails) -> Unit = {},
        onPayAnotherWayClick: () -> Unit = {},
        showBottomSheetContent: ((BottomSheetContent?) -> Unit)? = null,
    ) = composeTestRule.setContent {
        var bottomSheetContent by remember { mutableStateOf<BottomSheetContent?>(null) }
        val sheetState = rememberModalBottomSheetState(ModalBottomSheetValue.Hidden)
        val coroutineScope = rememberCoroutineScope()

        if (bottomSheetContent != null) {
            DisposableEffect(bottomSheetContent) {
                coroutineScope.launch { sheetState.show() }
                onDispose {
                    coroutineScope.launch { sheetState.hide() }
                }
            }
        }

        ModalBottomSheetLayout(
            sheetContent = bottomSheetContent ?: {
                // Must have some content at startup or bottom sheet crashes when
                // calculating its initial size
                Box(Modifier.defaultMinSize(minHeight = 1.dp)) {}
            },
            modifier = Modifier.fillMaxHeight(),
            sheetState = sheetState,
        ) {

            DefaultLinkTheme {
                WalletBody(
                    isProcessing = false,
                    paymentDetails = paymentDetails,
                    primaryButtonLabel = primaryButtonLabel,
                    errorMessage = errorMessage,
                    onAddNewPaymentMethodClick = onAddNewPaymentMethodClick,
                    onDeletePaymentMethod = onDeletePaymentMethod,
                    onPrimaryButtonClick = onPayButtonClick,
                    onPayAnotherWayClick = onPayAnotherWayClick,
                    showBottomSheetContent = showBottomSheetContent ?: {
                        bottomSheetContent = it
                        if (it == null) {
                            coroutineScope.launch { sheetState.hide() }
                        }
                    }
                )
            }
        }
    }

    private fun toggleListExpanded() =
        composeTestRule.onNodeWithTag("ChevronIcon", useUnmergedTree = true).performClick()

    private fun onPaymentDetailsItem(paymentDetails: ConsumerPaymentDetails.Card) =
        composeTestRule.onNodeWithText(paymentDetails.last4, substring = true)

    private fun onOptionsForPaymentMethod(paymentDetails: ConsumerPaymentDetails.Card) =
        onPaymentDetailsItem(paymentDetails).onChildren().filterToOne(hasContentDescription("Edit"))

    private fun onPrimaryButton() = composeTestRule.onNodeWithText(primaryButtonLabel)

    private fun onRemoveCardButton() = composeTestRule.onNodeWithText("Remove card")

    private fun onRemoveConfirmationDialog() =
        composeTestRule.onNodeWithText("Are you sure you want to remove this card?")

    private fun onCancelButton() = composeTestRule.onNodeWithText("Cancel")
    private fun onRemoveButton() = composeTestRule.onNodeWithText("Remove")
}
