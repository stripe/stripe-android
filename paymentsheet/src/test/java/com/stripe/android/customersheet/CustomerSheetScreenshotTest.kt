package com.stripe.android.customersheet

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.stripe.android.core.strings.resolvableString
import com.stripe.android.customersheet.ui.CustomerSheetScreen
import com.stripe.android.lpmfoundations.luxe.LpmRepositoryTestHelpers
import com.stripe.android.model.CardBrand
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.PaymentMethodFixtures
import com.stripe.android.paymentsheet.forms.FormFieldValues
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.paymentdatacollection.FormArguments
import com.stripe.android.paymentsheet.paymentdatacollection.ach.USBankAccountFormArguments
import com.stripe.android.paymentsheet.ui.DefaultEditPaymentMethodViewInteractor
import com.stripe.android.screenshottesting.FontSize
import com.stripe.android.screenshottesting.PaparazziRule
import com.stripe.android.screenshottesting.SystemAppearance
import com.stripe.android.testing.PaymentMethodFactory
import com.stripe.android.ui.core.cbc.CardBrandChoiceEligibility
import com.stripe.android.utils.screenshots.PaymentSheetAppearance
import org.junit.Rule
import org.junit.Test

internal class CustomerSheetScreenshotTest {
    @get:Rule
    val paparazzi = PaparazziRule(
        SystemAppearance.entries,
        listOf(FontSize.DefaultFont),
        listOf(PaymentSheetAppearance.DefaultAppearance),
        boxModifier = Modifier
            .padding(0.dp)
            .fillMaxWidth(),
    )

    private val usBankAccountFormArguments = USBankAccountFormArguments(
        showCheckbox = false,
        instantDebits = false,
        onBehalfOf = null,
        isCompleteFlow = false,
        isPaymentFlow = false,
        stripeIntentId = null,
        clientSecret = null,
        shippingDetails = null,
        draftPaymentSelection = null,
        onMandateTextChanged = { _, _ -> },
        onConfirmUSBankAccount = { },
        onCollectBankAccountResult = { },
        onUpdatePrimaryButtonState = { },
        onUpdatePrimaryButtonUIState = { },
        onError = { },
    )

    private val selectPaymentMethodViewState = CustomerSheetViewState.SelectPaymentMethod(
        title = null,
        savedPaymentMethods = listOf(PaymentMethodFixtures.CARD_PAYMENT_METHOD),
        paymentSelection = null,
        isLiveMode = false,
        isProcessing = false,
        isEditing = false,
        isGooglePayEnabled = false,
        primaryButtonVisible = false,
        primaryButtonLabel = null,
        cbcEligibility = CardBrandChoiceEligibility.Ineligible,
        allowsRemovalOfLastSavedPaymentMethod = true,
    )

    private val addPaymentMethodViewState = CustomerSheetViewState.AddPaymentMethod(
        paymentMethodCode = PaymentMethod.Type.Card.code,
        formFieldValues = FormFieldValues(
            showsMandate = false,
            userRequestedReuse = PaymentSelection.CustomerRequestedSave.RequestReuse,
        ),
        formElements = emptyList(),
        formArguments = FormArguments(
            paymentMethodCode = PaymentMethod.Type.Card.code,
            cbcEligibility = CardBrandChoiceEligibility.Ineligible,
            merchantName = ""
        ),
        usBankAccountFormArguments = usBankAccountFormArguments,
        supportedPaymentMethods = listOf(
            LpmRepositoryTestHelpers.card,
            LpmRepositoryTestHelpers.usBankAccount,
        ),
        enabled = true,
        isLiveMode = false,
        isProcessing = false,
        errorMessage = null,
        isFirstPaymentMethod = false,
        primaryButtonLabel = resolvableString("Save"),
        primaryButtonEnabled = false,
        customPrimaryButtonUiState = null,
        bankAccountResult = null,
        draftPaymentSelection = null,
        cbcEligibility = CardBrandChoiceEligibility.Ineligible,
    )

    @Test
    fun testDefault() {
        paparazzi.snapshot {
            CustomerSheetScreen(
                viewState = selectPaymentMethodViewState,
                paymentMethodNameProvider = { it!! },
            )
        }
    }

    @Test
    fun testWithPaymentMethods() {
        paparazzi.snapshot {
            val savedPaymentMethods = List(5) {
                PaymentMethod(
                    id = "pm_123$it",
                    created = null,
                    code = "card",
                    liveMode = false,
                    type = PaymentMethod.Type.Card,
                    card = PaymentMethod.Card(
                        brand = CardBrand.orderedBrands[it],
                        last4 = "424$it",
                    )
                )
            }
            var counter = 0
            CustomerSheetScreen(
                viewState = selectPaymentMethodViewState.copy(
                    title = "Screenshot testing",
                    savedPaymentMethods = savedPaymentMethods,
                    paymentSelection = PaymentSelection.Saved(
                        savedPaymentMethods.first()
                    ),
                    primaryButtonLabel = "Continue",
                    errorMessage = "This is an error message.",
                ),
                paymentMethodNameProvider = {
                    counter++
                    "424$counter"
                },
            )
        }
    }

    @Test
    fun testEditModeWithPaymentMethods() {
        paparazzi.snapshot {
            val savedPaymentMethods = List(5) {
                PaymentMethod(
                    id = "pm_123$it",
                    created = null,
                    code = "card",
                    liveMode = false,
                    type = PaymentMethod.Type.Card,
                    card = PaymentMethod.Card(
                        brand = CardBrand.orderedBrands[it],
                        last4 = "424$it",
                    )
                )
            }
            var counter = 0
            CustomerSheetScreen(
                selectPaymentMethodViewState.copy(
                    title = "Screenshot testing",
                    savedPaymentMethods = savedPaymentMethods,
                    paymentSelection = PaymentSelection.Saved(
                        savedPaymentMethods.first()
                    ),
                    isEditing = true,
                    isGooglePayEnabled = true,
                    primaryButtonLabel = "Continue",
                    errorMessage = "This is an error message.",
                ),
                paymentMethodNameProvider = {
                    counter++
                    "424$counter"
                },
            )
        }
    }

    @Test
    fun testGooglePay() {
        paparazzi.snapshot {
            CustomerSheetScreen(
                viewState = selectPaymentMethodViewState.copy(
                    title = "Screenshot testing",
                    paymentSelection = PaymentSelection.GooglePay,
                    isGooglePayEnabled = true,
                    primaryButtonLabel = "Continue",
                    errorMessage = "This is an error message.",
                ),
                paymentMethodNameProvider = { it!! },
            )
        }
    }

    @Test
    fun testSelectUSBankAccount() {
        paparazzi.snapshot {
            CustomerSheetScreen(
                viewState = selectPaymentMethodViewState.copy(
                    title = "Screenshot testing",
                    savedPaymentMethods = listOf(
                        PaymentMethodFixtures.CARD_PAYMENT_METHOD,
                        PaymentMethodFixtures.US_BANK_ACCOUNT,
                    ),
                    paymentSelection = PaymentSelection.Saved(
                        PaymentMethodFixtures.US_BANK_ACCOUNT
                    ),
                    isGooglePayEnabled = false,
                    primaryButtonLabel = "Continue",
                    primaryButtonVisible = true,
                    mandateText = "Some mandate text."
                ),
                paymentMethodNameProvider = { it!! },
            )
        }
    }

    @Test
    fun testMandateAbovePrimaryButton() {
        paparazzi.snapshot {
            CustomerSheetScreen(
                viewState = addPaymentMethodViewState.copy(
                    paymentMethodCode = PaymentMethod.Type.Card.code,
                    isFirstPaymentMethod = true,
                    primaryButtonEnabled = false,
                    mandateText = "This is a mandate.",
                    showMandateAbovePrimaryButton = true,
                ),
                paymentMethodNameProvider = { it!! },
                displayAddForm = false,
            )
        }
    }

    @Test
    fun testMandateBelowPrimaryButton() {
        paparazzi.snapshot {
            CustomerSheetScreen(
                viewState = addPaymentMethodViewState.copy(
                    paymentMethodCode = PaymentMethod.Type.Card.code,
                    isFirstPaymentMethod = true,
                    primaryButtonEnabled = false,
                    mandateText = "This is a mandate.",
                    showMandateAbovePrimaryButton = false,
                ),
                paymentMethodNameProvider = { it!! },
                displayAddForm = false,
            )
        }
    }

    @Test
    fun testConfirmCloseDialog() {
        paparazzi.snapshot {
            CustomerSheetScreen(
                viewState = addPaymentMethodViewState.copy(
                    paymentMethodCode = PaymentMethod.Type.USBankAccount.code,
                    displayDismissConfirmationModal = true,
                ),
                paymentMethodNameProvider = { it!! },
                displayAddForm = false,
            )
        }
    }

    @Test
    fun testEditScreen() {
        val paymentMethod = PaymentMethodFactory.card().copy(
            card = PaymentMethod.Card(
                last4 = "1001",
                networks = PaymentMethod.Card.Networks(
                    available = setOf(CardBrand.CartesBancaires.code, CardBrand.Visa.code),
                ),
            )
        )

        val editPaymentMethod = CustomerSheetViewState.EditPaymentMethod(
            editPaymentMethodInteractor = DefaultEditPaymentMethodViewInteractor(
                initialPaymentMethod = paymentMethod,
                displayName = "Card",
                removeExecutor = { null },
                updateExecutor = { pm, _ -> Result.success(pm) },
                eventHandler = {},
                canRemove = true,
            ),
            isLiveMode = true,
            cbcEligibility = CardBrandChoiceEligibility.Eligible(preferredNetworks = emptyList()),
            savedPaymentMethods = emptyList(),
            allowsRemovalOfLastSavedPaymentMethod = true,
        )

        paparazzi.snapshot {
            CustomerSheetScreen(
                viewState = editPaymentMethod,
                paymentMethodNameProvider = { it!! },
            )
        }
    }

    @Test
    fun testEditScreenWithoutRemove() {
        val paymentMethod = PaymentMethodFactory.card().copy(
            card = PaymentMethod.Card(
                last4 = "1001",
                networks = PaymentMethod.Card.Networks(
                    available = setOf(CardBrand.CartesBancaires.code, CardBrand.Visa.code),
                ),
            )
        )

        val editPaymentMethod = CustomerSheetViewState.EditPaymentMethod(
            editPaymentMethodInteractor = DefaultEditPaymentMethodViewInteractor(
                initialPaymentMethod = paymentMethod,
                displayName = "Card",
                removeExecutor = { null },
                updateExecutor = { pm, _ -> Result.success(pm) },
                eventHandler = {},
                canRemove = false,
            ),
            isLiveMode = true,
            cbcEligibility = CardBrandChoiceEligibility.Eligible(preferredNetworks = emptyList()),
            savedPaymentMethods = emptyList(),
            allowsRemovalOfLastSavedPaymentMethod = false,
        )

        paparazzi.snapshot {
            CustomerSheetScreen(
                viewState = editPaymentMethod,
                paymentMethodNameProvider = { it!! },
            )
        }
    }
}
