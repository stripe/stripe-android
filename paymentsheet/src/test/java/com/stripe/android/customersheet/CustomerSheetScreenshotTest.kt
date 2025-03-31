package com.stripe.android.customersheet

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.stripe.android.DefaultCardBrandFilter
import com.stripe.android.core.strings.resolvableString
import com.stripe.android.customersheet.ui.CustomerSheetScreen
import com.stripe.android.lpmfoundations.luxe.LpmRepositoryTestHelpers
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadataFactory
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodSaveConsentBehavior
import com.stripe.android.lpmfoundations.paymentmethod.UiDefinitionFactory
import com.stripe.android.model.CardBrand
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.PaymentMethodFixtures
import com.stripe.android.payments.bankaccount.CollectBankAccountLauncher
import com.stripe.android.payments.financialconnections.FinancialConnectionsAvailability
import com.stripe.android.paymentsheet.forms.FormFieldValues
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.paymentdatacollection.FormArguments
import com.stripe.android.paymentsheet.paymentdatacollection.ach.USBankAccountFormArguments
import com.stripe.android.paymentsheet.ui.DefaultUpdatePaymentMethodInteractor
import com.stripe.android.paymentsheet.utils.ViewModelStoreOwnerContext
import com.stripe.android.screenshottesting.FontSize
import com.stripe.android.screenshottesting.PaparazziRule
import com.stripe.android.screenshottesting.SystemAppearance
import com.stripe.android.testing.CoroutineTestRule
import com.stripe.android.testing.FakeErrorReporter
import com.stripe.android.testing.SetupIntentFactory
import com.stripe.android.ui.core.cbc.CardBrandChoiceEligibility
import com.stripe.android.utils.NullCardAccountRangeRepositoryFactory
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

    @get:Rule
    val paparazziWithEveryAppearance = PaparazziRule(
        SystemAppearance.entries,
        FontSize.entries,
        PaymentSheetAppearance.entries,
        boxModifier = Modifier
            .padding(0.dp)
            .fillMaxWidth(),
    )

    @get:Rule
    val coroutineRule = CoroutineTestRule()

    private val usBankAccountFormArguments = USBankAccountFormArguments(
        showCheckbox = false,
        instantDebits = false,
        incentive = null,
        linkMode = null,
        onBehalfOf = null,
        isCompleteFlow = false,
        isPaymentFlow = false,
        stripeIntentId = null,
        clientSecret = null,
        shippingDetails = null,
        draftPaymentSelection = null,
        hostedSurface = CollectBankAccountLauncher.HOSTED_SURFACE_CUSTOMER_SHEET,
        onMandateTextChanged = { _, _ -> },
        onLinkedBankAccountChanged = { },
        onUpdatePrimaryButtonState = { },
        onUpdatePrimaryButtonUIState = { },
        onError = { },
        setAsDefaultPaymentMethodEnabled = false,
        financialConnectionsAvailability = FinancialConnectionsAvailability.Full,
        setAsDefaultMatchesSaveForFutureUse = false,
    )

    private val selectPaymentMethodViewState = CustomerSheetViewState.SelectPaymentMethod(
        title = null,
        savedPaymentMethods = listOf(PaymentMethodFixtures.CARD_PAYMENT_METHOD),
        paymentSelection = null,
        isLiveMode = false,
        isProcessing = false,
        isEditing = false,
        showGooglePay = false,
        primaryButtonVisible = false,
        canEdit = true,
        canRemovePaymentMethods = true,
        isCbcEligible = false,
    )

    private val addPaymentMethodViewState = CustomerSheetViewState.AddPaymentMethod(
        paymentMethodCode = PaymentMethod.Type.Card.code,
        formFieldValues = FormFieldValues(
            userRequestedReuse = PaymentSelection.CustomerRequestedSave.RequestReuse,
        ),
        formElements = PaymentMethodMetadataFactory.create(
            stripeIntent = SetupIntentFactory.create()
        ).formElementsForCode(
            code = PaymentMethod.Type.Card.code,
            uiDefinitionFactoryArgumentsFactory = UiDefinitionFactory.Arguments.Factory.Default(
                cardAccountRangeRepositoryFactory = NullCardAccountRangeRepositoryFactory,
                linkConfigurationCoordinator = null,
                onLinkInlineSignupStateChanged = {},
            )
        ) ?: listOf(),
        formArguments = FormArguments(
            paymentMethodCode = PaymentMethod.Type.Card.code,
            cbcEligibility = CardBrandChoiceEligibility.Ineligible,
            merchantName = "",
            hasIntentToSetup = true,
            paymentMethodSaveConsentBehavior = PaymentMethodSaveConsentBehavior.Legacy,
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
        primaryButtonLabel = "Save".resolvableString,
        primaryButtonEnabled = false,
        customPrimaryButtonUiState = null,
        bankAccountSelection = null,
        draftPaymentSelection = null,
        errorReporter = FakeErrorReporter(),
    )

    @Test
    fun testDefault() {
        paparazzi.snapshot {
            CustomerSheetScreen(
                viewState = selectPaymentMethodViewState,
                paymentMethodNameProvider = { it!!.resolvableString },
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
                    errorMessage = "This is an error message.",
                ),
                paymentMethodNameProvider = {
                    counter++
                    "424$counter".resolvableString
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
                    showGooglePay = true,
                    errorMessage = "This is an error message.",
                ),
                paymentMethodNameProvider = {
                    counter++
                    "424$counter".resolvableString
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
                    showGooglePay = true,
                    errorMessage = "This is an error message.",
                ),
                paymentMethodNameProvider = { it!!.resolvableString },
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
                    showGooglePay = false,
                    primaryButtonVisible = true,
                    mandateText = "Some mandate text.".resolvableString
                ),
                paymentMethodNameProvider = { it!!.resolvableString },
            )
        }
    }

    @Test
    fun testAddCardForm() {
        paparazzi.snapshot {
            ViewModelStoreOwnerContext {
                CustomerSheetScreen(
                    viewState = addPaymentMethodViewState,
                    paymentMethodNameProvider = { it!!.resolvableString },
                )
            }
        }
    }

    @Test
    fun testAddCardFormWithError() {
        paparazzi.snapshot {
            ViewModelStoreOwnerContext {
                CustomerSheetScreen(
                    viewState = addPaymentMethodViewState.copy(
                        errorMessage = resolvableString("Something went wrong!")
                    ),
                    paymentMethodNameProvider = { it!!.resolvableString },
                )
            }
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
                    mandateText = "This is a mandate.".resolvableString,
                    showMandateAbovePrimaryButton = true,
                ),
                paymentMethodNameProvider = { it!!.resolvableString },
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
                    mandateText = "This is a mandate.".resolvableString,
                    showMandateAbovePrimaryButton = false,
                ),
                paymentMethodNameProvider = { it!!.resolvableString },
                displayAddForm = false,
            )
        }
    }

    @Test
    fun testConfirmCloseDialog() {
        paparazziWithEveryAppearance.snapshot {
            CustomerSheetScreen(
                viewState = addPaymentMethodViewState.copy(
                    paymentMethodCode = PaymentMethod.Type.USBankAccount.code,
                    displayDismissConfirmationModal = true,
                ),
                paymentMethodNameProvider = { it!!.resolvableString },
                displayAddForm = false,
            )
        }
    }

    @Test
    fun testUpdatePaymentMethodScreen() {
        paparazzi.snapshot {
            CustomerSheetScreen(
                viewState = createUpdatePaymentMethodViewState(canRemove = true),
                paymentMethodNameProvider = { it!!.resolvableString },
            )
        }
    }

    @Test
    fun testUpdatePaymentMethodScreen_withoutRemove() {
        paparazzi.snapshot {
            CustomerSheetScreen(
                viewState = createUpdatePaymentMethodViewState(canRemove = false),
                paymentMethodNameProvider = { it!!.resolvableString },
            )
        }
    }

    private fun createUpdatePaymentMethodViewState(
        canRemove: Boolean,
    ): CustomerSheetViewState {
        return CustomerSheetViewState.UpdatePaymentMethod(
            updatePaymentMethodInteractor = DefaultUpdatePaymentMethodInteractor(
                displayableSavedPaymentMethod = PaymentMethodFixtures.displayableCard(),
                removeExecutor = { null },
                updatePaymentMethodExecutor = { paymentMethod, _ -> Result.success(paymentMethod) },
                setDefaultPaymentMethodExecutor = { _ -> Result.success(Unit) },
                canRemove = canRemove,
                isLiveMode = true,
                cardBrandFilter = DefaultCardBrandFilter,
                onBrandChoiceSelected = {},
                // This checkbox is never displayed in CustomerSheet.
                shouldShowSetAsDefaultCheckbox = false,
                isDefaultPaymentMethod = false,
                onUpdateSuccess = {},
            ),
            isLiveMode = true,
        )
    }
}
