package com.stripe.android.customersheet

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.stripe.android.customersheet.ui.CustomerSheetScreen
import com.stripe.android.model.CardBrand
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.PaymentMethodFixtures
import com.stripe.android.paymentsheet.forms.FormFieldValues
import com.stripe.android.paymentsheet.forms.FormViewModel
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.paymentdatacollection.FormArguments
import com.stripe.android.paymentsheet.paymentdatacollection.ach.USBankAccountFormArguments
import com.stripe.android.testing.FeatureFlagTestRule
import com.stripe.android.ui.core.cbc.CardBrandChoiceEligibility
import com.stripe.android.ui.core.forms.resources.LpmRepository
import com.stripe.android.uicore.elements.IdentifierSpec
import com.stripe.android.uicore.forms.FormFieldEntry
import com.stripe.android.utils.FeatureFlags
import com.stripe.android.utils.screenshots.FontSize
import com.stripe.android.utils.screenshots.PaparazziRule
import com.stripe.android.utils.screenshots.PaymentSheetAppearance
import com.stripe.android.utils.screenshots.SystemAppearance
import org.junit.Rule
import org.junit.Test

internal class CustomerSheetScreenshotTest {
    @get:Rule
    val paparazzi = PaparazziRule(
        SystemAppearance.values(),
        arrayOf(FontSize.DefaultFont),
        arrayOf(PaymentSheetAppearance.DefaultAppearance),
        boxModifier = Modifier
            .padding(0.dp)
            .fillMaxWidth(),
    )

    @get:Rule
    val featureFlagTestRule = FeatureFlagTestRule(
        featureFlag = FeatureFlags.customerSheetACHv2,
        isEnabled = false,
    )

    private val usBankAccountFormArguments = USBankAccountFormArguments(
        onBehalfOf = null,
        isCompleteFlow = false,
        isPaymentFlow = false,
        stripeIntentId = null,
        clientSecret = null,
        shippingDetails = null,
        draftPaymentSelection = null,
        onMandateTextChanged = { _, _ -> },
        onHandleUSBankAccount = { },
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
    )

    private val addPaymentMethodViewState = CustomerSheetViewState.AddPaymentMethod(
        paymentMethodCode = PaymentMethod.Type.Card.code,
        formViewData = FormViewModel.ViewData(
            completeFormValues = FormFieldValues(
                showsMandate = false,
                userRequestedReuse = PaymentSelection.CustomerRequestedSave.RequestReuse,
            ),
        ),
        formArguments = FormArguments(
            paymentMethodCode = PaymentMethod.Type.Card.code,
            showCheckbox = false,
            showCheckboxControlledFields = false,
            cbcEligibility = CardBrandChoiceEligibility.Ineligible,
            merchantName = ""
        ),
        usBankAccountFormArguments = usBankAccountFormArguments,
        supportedPaymentMethods = listOf(
            LpmRepository.HardcodedCard,
            LpmRepository.hardCodedUsBankAccount,
        ),
        selectedPaymentMethod = LpmRepository.HardcodedCard,
        enabled = true,
        isLiveMode = false,
        isProcessing = false,
        errorMessage = null,
        isFirstPaymentMethod = false,
        primaryButtonLabel = "Save",
    )

    @Test
    fun testDefault() {
        paparazzi.snapshot {
            CustomerSheetScreen(
                viewState = selectPaymentMethodViewState,
                paymentMethodNameProvider = { it!! },
                formViewModelSubComponentBuilderProvider = null,
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
                formViewModelSubComponentBuilderProvider = null,
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
                formViewModelSubComponentBuilderProvider = null,
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
                formViewModelSubComponentBuilderProvider = null,
            )
        }
    }

    @Test
    fun testAddPaymentMethodDisabled() {
        paparazzi.snapshot {
            CustomerSheetScreen(
                viewState = addPaymentMethodViewState.copy(
                    paymentMethodCode = PaymentMethod.Type.Card.code,
                    formViewData = FormViewModel.ViewData(),
                    errorMessage = "This is an error message.",
                ),
                paymentMethodNameProvider = { it!! },
                formViewModelSubComponentBuilderProvider = null,
            )
        }
    }

    @Test
    fun testAddPaymentMethodEnabled() {
        paparazzi.snapshot {
            CustomerSheetScreen(
                viewState = addPaymentMethodViewState.copy(
                    paymentMethodCode = PaymentMethod.Type.Card.code,
                    formViewData = FormViewModel.ViewData(
                        completeFormValues = FormFieldValues(
                            fieldValuePairs = mapOf(
                                IdentifierSpec.Generic("test") to FormFieldEntry("test", true)
                            ),
                            showsMandate = true,
                            userRequestedReuse = PaymentSelection.CustomerRequestedSave.RequestNoReuse
                        )
                    ),
                ),
                paymentMethodNameProvider = { it!! },
                formViewModelSubComponentBuilderProvider = null,
            )
        }
    }

    @Test
    fun testAddFirstPaymentMethodDisabled() {
        paparazzi.snapshot {
            CustomerSheetScreen(
                viewState = addPaymentMethodViewState.copy(
                    paymentMethodCode = PaymentMethod.Type.Card.code,
                    formViewData = FormViewModel.ViewData(),
                    errorMessage = "This is an error message.",
                    isFirstPaymentMethod = true
                ),
                paymentMethodNameProvider = { it!! },
                formViewModelSubComponentBuilderProvider = null,
            )
        }
    }

    @Test
    fun testAddFirstPaymentMethodEnabled() {
        paparazzi.snapshot {
            CustomerSheetScreen(
                viewState = addPaymentMethodViewState.copy(
                    paymentMethodCode = PaymentMethod.Type.Card.code,
                    formViewData = FormViewModel.ViewData(
                        completeFormValues = FormFieldValues(
                            fieldValuePairs = mapOf(
                                IdentifierSpec.Generic("test") to FormFieldEntry("test", true)
                            ),
                            showsMandate = true,
                            userRequestedReuse = PaymentSelection.CustomerRequestedSave.RequestNoReuse
                        )
                    ),
                    isFirstPaymentMethod = true
                ),
                paymentMethodNameProvider = { it!! },
                formViewModelSubComponentBuilderProvider = null,
            )
        }
    }
}
