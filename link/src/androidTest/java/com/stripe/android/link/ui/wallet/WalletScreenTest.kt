package com.stripe.android.link.ui.wallet

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.ModalBottomSheetLayout
import androidx.compose.material.ModalBottomSheetValue
import androidx.compose.material.rememberModalBottomSheetState
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.filter
import androidx.compose.ui.test.filterToOne
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onChildren
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onParent
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import com.stripe.android.link.theme.DefaultLinkTheme
import com.stripe.android.link.ui.BottomSheetContent
import com.stripe.android.link.ui.ErrorMessage
import com.stripe.android.link.ui.paymentmethod.SupportedPaymentMethod
import com.stripe.android.model.CardBrand
import com.stripe.android.model.ConsumerPaymentDetails
import com.stripe.android.model.CvcCheck
import com.stripe.android.ui.core.elements.CvcController
import com.stripe.android.uicore.elements.DateConfig
import com.stripe.android.uicore.elements.SimpleTextFieldController
import com.stripe.android.uicore.elements.TextFieldController
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@OptIn(ExperimentalMaterialApi::class)
@RunWith(AndroidJUnit4::class)
internal class WalletScreenTest {
    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    private val primaryButtonLabel = "Pay $10.99"
    private val paymentDetails = listOf(
        ConsumerPaymentDetails.Card(
            id = "id1",
            isDefault = true,
            expiryYear = 2026,
            expiryMonth = 12,
            brand = CardBrand.Visa,
            last4 = "4242",
            cvcCheck = CvcCheck.Pass
        ),
        ConsumerPaymentDetails.Card(
            id = "id2",
            isDefault = false,
            expiryYear = 2020,
            expiryMonth = 11,
            brand = CardBrand.MasterCard,
            last4 = "4444",
            cvcCheck = CvcCheck.Fail
        ),
        ConsumerPaymentDetails.Card(
            id = "id3",
            isDefault = false,
            expiryYear = 2026,
            expiryMonth = 11,
            brand = CardBrand.AmericanExpress,
            last4 = "0005",
            cvcCheck = CvcCheck.Unchecked
        ),
        ConsumerPaymentDetails.BankAccount(
            id = "id4",
            isDefault = false,
            bankIconCode = "icon",
            bankName = "Stripe Bank",
            last4 = "6789"
        ),
        ConsumerPaymentDetails.BankAccount(
            id = "id5",
            isDefault = false,
            bankIconCode = "icon2",
            bankName = "Stripe Credit Union",
            last4 = "1234"
        )
    )
    private val paymentDetailsFlow = MutableStateFlow(paymentDetails)

    @Test
    fun selected_payment_method_is_shown_when_collapsed() {
        val initiallySelectedItem = paymentDetails[4]
        setContent(
            isExpanded = false,
            selectedItem = initiallySelectedItem
        )

        composeTestRule.onNodeWithText("Payment").onParent().onChildren()
            .filter(hasText(initiallySelectedItem.label, substring = true))
            .assertCountEquals(1)
    }

    @Test
    fun expand_list_triggers_callback() {
        var expanded: Boolean? = null
        setContent(
            isExpanded = false,
            setExpanded = {
                expanded = it
            }
        )
        assertCollapsed()
        composeTestRule.onNodeWithText("Payment").performClick()
        assertThat(expanded).isTrue()
    }

    @Test
    fun collapse_list_triggers_callback() {
        var expanded: Boolean? = null
        setContent(
            isExpanded = true,
            setExpanded = {
                expanded = it
            }
        )
        assertExpanded()
        composeTestRule.onNodeWithText("Payment methods").performClick()
        assertThat(expanded).isFalse()
    }

    @Test
    fun when_no_payment_option_is_selected_then_primary_button_is_disabled() {
        var payButtonClickCount = 0

        setContent(
            selectedItem = null,
            onPayButtonClick = {
                payButtonClickCount++
            }
        )

        onPrimaryButton().assertIsNotEnabled()
        onPrimaryButton().performClick()
        assertThat(payButtonClickCount).isEqualTo(0)
    }

    @Test
    fun when_card_is_not_supported_then_cards_cannot_be_selected() {
        var selectedItem: ConsumerPaymentDetails.PaymentDetails? = null
        setContent(
            supportedTypes = setOf(ConsumerPaymentDetails.BankAccount.type),
            selectedItem = paymentDetails.first(),
            onItemSelected = {
                selectedItem = it
            }
        )

        assertExpanded()
        assertThat(selectedItem).isNull()
        onPrimaryButton().assertIsNotEnabled()

        onPaymentDetailsItem(paymentDetails[1]).assertIsNotEnabled().performClick()

        assertThat(selectedItem).isNull()
        onPrimaryButton().assertIsNotEnabled()

        onPaymentDetailsItem(paymentDetails[2]).assertIsNotEnabled().performClick()

        assertThat(selectedItem).isNull()
        onPrimaryButton().assertIsNotEnabled()
    }

    @Test
    fun when_bank_account_is_not_supported_then_bank_accounts_cannot_be_selected() {
        var selectedItem: ConsumerPaymentDetails.PaymentDetails? = null
        setContent(
            supportedTypes = setOf(ConsumerPaymentDetails.Card.type),
            selectedItem = paymentDetails[3],
            onItemSelected = {
                selectedItem = it
            }
        )

        assertExpanded()
        assertThat(selectedItem).isNull()
        onPrimaryButton().assertIsNotEnabled()

        onPaymentDetailsItem(paymentDetails[4]).assertIsNotEnabled().performClick()

        assertThat(selectedItem).isNull()
        onPrimaryButton().assertIsNotEnabled()
    }

    @Test
    fun when_payment_method_is_not_supported_then_error_message_is_shown() {
        var selectedItem: ConsumerPaymentDetails.PaymentDetails? = null
        setContent(
            supportedTypes = emptySet(),
            selectedItem = paymentDetails.first(),
            onItemSelected = {
                selectedItem = it
            }
        )

        assertExpanded()
        assertThat(selectedItem).isNull()
        onPrimaryButton().assertIsNotEnabled()

        composeTestRule.onAllNodesWithText("Unavailable for this purchase")
            .assertCountEquals(5)
    }

    @Test
    fun when_no_selected_payment_method_then_wallet_is_expanded() {
        var selectedItem: ConsumerPaymentDetails.PaymentDetails? = null
        setContent(
            selectedItem = null,
            isExpanded = false,
            onItemSelected = {
                selectedItem = it
            }
        )

        assertExpanded()
        assertThat(selectedItem).isNull()
        onPrimaryButton().assertIsNotEnabled()
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
        composeTestRule.onNodeWithText("Add a payment method").performClick()

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
    fun card_options_menu_shows_correct_options() {
        setContent()
        toggleListExpanded()
        onOptionsForPaymentMethod(paymentDetails.first()).performClick()
        onRemoveCardButton().assertExists()
        onUpdateCardButton().assertExists()
        onCancelButton().assertExists()
        onRemoveAccountButton().assertDoesNotExist()
    }

    @Test
    fun bank_account_options_menu_shows_correct_options() {
        setContent()
        toggleListExpanded()
        onOptionsForPaymentMethod(paymentDetails[3]).performClick()
        onRemoveAccountButton().assertExists()
        onCancelButton().assertExists()
        onRemoveCardButton().assertDoesNotExist()
        onUpdateCardButton().assertDoesNotExist()
    }

    @Test
    fun delete_card_shows_dialog_confirmation() {
        setContent()
        toggleListExpanded()
        onOptionsForPaymentMethod(paymentDetails.first()).performClick()
        onRemoveCardButton().assertExists()
        onRemoveCardButton().performClick()
        onRemoveCardConfirmationDialog().assertExists()
    }

    @Test
    fun delete_bank_account_shows_dialog_confirmation() {
        setContent()
        toggleListExpanded()
        onOptionsForPaymentMethod(paymentDetails[3]).performClick()
        onRemoveAccountButton().assertExists()
        onRemoveAccountButton().performClick()
        onRemoveBankAccountConfirmationDialog().assertExists()
    }

    @Test
    fun update_card_triggers_callback() {
        var paymentMethod: ConsumerPaymentDetails.PaymentDetails? = null
        setContent(
            onEditPaymentMethod = {
                paymentMethod = it
            }
        )
        toggleListExpanded()
        onOptionsForPaymentMethod(paymentDetails.first()).performClick()
        onUpdateCardButton().assertExists()
        onUpdateCardButton().performClick()
        assertThat(paymentMethod).isEqualTo(paymentDetails.first())
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
        onRemoveCardConfirmationDialog().assertExists()
    }

    @Test
    fun when_error_message_is_not_null_then_it_is_visible() {
        val errorMessage = "Error message"
        setContent(errorMessage = ErrorMessage.Raw(errorMessage))
        composeTestRule.onNodeWithText(errorMessage).assertExists()
    }

    @Test
    fun when_no_payment_method_is_selected_doesnt_show_expiry_date_form() {
        setContent(
            isExpanded = false,
            selectedItem = null
        )

        composeTestRule.onNodeWithText("MM / YY").assertDoesNotExist()
        composeTestRule.onNodeWithText("CVC").assertDoesNotExist()
    }

    @Test
    fun when_bank_account_is_selected_doesnt_show_expiry_date_form() {
        setContent(
            isExpanded = false,
            selectedItem = paymentDetails[4]
        )

        composeTestRule.onNodeWithText("MM / YY").assertDoesNotExist()
        composeTestRule.onNodeWithText("CVC").assertDoesNotExist()
    }

    @Test
    fun when_selected_card_is_not_expired_doesnt_show_expiry_date_form() {
        val initiallySelectedItem = paymentDetails[0]
        setContent(
            isExpanded = false,
            selectedItem = initiallySelectedItem
        )

        composeTestRule.onNodeWithText("MM / YY").assertDoesNotExist()
        composeTestRule.onNodeWithText("CVC").assertDoesNotExist()
    }

    @Test
    fun when_selected_card_is_expired_shows_expiry_date_form() {
        val initiallySelectedItem = paymentDetails[1]
        setContent(
            isExpanded = false,
            selectedItem = initiallySelectedItem
        )

        composeTestRule.onNodeWithText("MM / YY")
            .assertExists()
            .assertIsEnabled()

        composeTestRule.onNodeWithText("CVC")
            .assertExists()
            .assertIsEnabled()
    }

    private fun setContent(
        supportedTypes: Set<String> = SupportedPaymentMethod.allTypes,
        selectedItem: ConsumerPaymentDetails.PaymentDetails? = paymentDetails.first(),
        isExpanded: Boolean = true,
        errorMessage: ErrorMessage? = null,
        expiryDateController: TextFieldController = SimpleTextFieldController(DateConfig()),
        cvcController: CvcController = CvcController(cardBrandFlow = flowOf(CardBrand.Visa)),
        setExpanded: (Boolean) -> Unit = {},
        onItemSelected: (ConsumerPaymentDetails.PaymentDetails) -> Unit = {},
        onAddNewPaymentMethodClick: () -> Unit = {},
        onEditPaymentMethod: (ConsumerPaymentDetails.PaymentDetails) -> Unit = {},
        onSetDefault: (ConsumerPaymentDetails.PaymentDetails) -> Unit = {},
        onDeletePaymentMethod: (ConsumerPaymentDetails.PaymentDetails) -> Unit = {},
        onPayButtonClick: () -> Unit = {},
        onPayAnotherWayClick: () -> Unit = {},
        showBottomSheetContent: ((BottomSheetContent?) -> Unit)? = null
    ) = composeTestRule.setContent {
        var bottomSheetContent by remember { mutableStateOf<BottomSheetContent?>(null) }
        val sheetState = rememberModalBottomSheetState(ModalBottomSheetValue.Hidden)
        val coroutineScope = rememberCoroutineScope()
        val paymentDetailsList by paymentDetailsFlow.collectAsState()

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
            sheetState = sheetState
        ) {
            DefaultLinkTheme {
                WalletBody(
                    uiState = WalletUiState(
                        paymentDetailsList = paymentDetailsList,
                        supportedTypes = supportedTypes,
                        selectedItem = selectedItem,
                        isExpanded = isExpanded,
                        errorMessage = errorMessage
                    ),
                    primaryButtonLabel = primaryButtonLabel,
                    expiryDateController = expiryDateController,
                    cvcController = cvcController,
                    setExpanded = setExpanded,
                    onItemSelected = onItemSelected,
                    onAddNewPaymentMethodClick = onAddNewPaymentMethodClick,
                    onEditPaymentMethod = onEditPaymentMethod,
                    onSetDefault = onSetDefault,
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

    private fun onPaymentDetailsItem(paymentDetails: ConsumerPaymentDetails.PaymentDetails) =
        composeTestRule.onNodeWithText(paymentDetails.label, substring = true)

    private fun onOptionsForPaymentMethod(paymentDetails: ConsumerPaymentDetails.PaymentDetails) =
        onPaymentDetailsItem(paymentDetails).onChildren().filterToOne(hasContentDescription("Edit"))

    // Assert list is expanded or collapsed based on the header text
    private fun assertCollapsed() = composeTestRule.onNodeWithText("Payment").assertExists()
    private fun assertExpanded() = composeTestRule.onNodeWithText("Payment methods").assertExists()

    private fun onPrimaryButton() = composeTestRule.onNodeWithText(primaryButtonLabel)

    private fun onRemoveCardButton() = composeTestRule.onNodeWithText("Remove card")
    private fun onUpdateCardButton() = composeTestRule.onNodeWithText("Update card")
    private fun onRemoveAccountButton() = composeTestRule.onNodeWithText("Remove linked account")

    private fun onRemoveCardConfirmationDialog() =
        composeTestRule.onNodeWithText("Are you sure you want to remove this card?")

    private fun onRemoveBankAccountConfirmationDialog() =
        composeTestRule.onNodeWithText("Are you sure you want to remove this linked account?")

    private fun onCancelButton() = composeTestRule.onNodeWithText("Cancel")
    private fun onRemoveButton() = composeTestRule.onNodeWithText("Remove")

    private val ConsumerPaymentDetails.PaymentDetails.label
        get() = when (this) {
            is ConsumerPaymentDetails.Card -> last4
            is ConsumerPaymentDetails.BankAccount -> last4
        }
}
