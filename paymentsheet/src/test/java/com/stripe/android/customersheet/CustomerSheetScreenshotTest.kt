package com.stripe.android.customersheet

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.stripe.android.customersheet.ui.CustomerSheetScreen
import com.stripe.android.customersheet.utils.CustomerSheetTestHelper.addPaymentMethodViewState
import com.stripe.android.customersheet.utils.CustomerSheetTestHelper.mockedFormViewModel
import com.stripe.android.customersheet.utils.CustomerSheetTestHelper.selectPaymentMethodViewState
import com.stripe.android.model.CardBrand
import com.stripe.android.model.PaymentMethod
import com.stripe.android.paymentsheet.forms.FormFieldValues
import com.stripe.android.paymentsheet.forms.FormViewModel
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.uicore.elements.IdentifierSpec
import com.stripe.android.uicore.forms.FormFieldEntry
import com.stripe.android.utils.screenshots.FontSize
import com.stripe.android.utils.screenshots.PaparazziRule
import com.stripe.android.utils.screenshots.PaymentSheetAppearance
import com.stripe.android.utils.screenshots.SystemAppearance
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCustomerSheetApi::class)
class CustomerSheetScreenshotTest {
    @get:Rule
    val paparazzi = PaparazziRule(
        SystemAppearance.values(),
        arrayOf(FontSize.DefaultFont),
        arrayOf(PaymentSheetAppearance.DefaultAppearance),
        boxModifier = Modifier
            .padding(0.dp)
            .fillMaxWidth(),
    )

    private val configuration = CustomerSheet.Configuration()

    @Test
    fun testDefault() {
        paparazzi.snapshot {
            CustomerSheetScreen(
                viewState = selectPaymentMethodViewState,
                paymentMethodNameProvider = { it!! },
                formViewModelSubComponentBuilderProvider = mockedFormViewModel(
                    configuration = configuration,
                ),
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
                formViewModelSubComponentBuilderProvider = mockedFormViewModel(
                    configuration = configuration,
                ),
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
                formViewModelSubComponentBuilderProvider = mockedFormViewModel(
                    configuration = configuration,
                ),
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
                formViewModelSubComponentBuilderProvider = mockedFormViewModel(
                    configuration = configuration,
                ),
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
                formViewModelSubComponentBuilderProvider = mockedFormViewModel(
                    configuration = configuration,
                ),
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
                formViewModelSubComponentBuilderProvider = mockedFormViewModel(
                    configuration = configuration,
                ),
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
                formViewModelSubComponentBuilderProvider = mockedFormViewModel(
                    configuration = configuration,
                ),
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
                formViewModelSubComponentBuilderProvider = mockedFormViewModel(
                    configuration = configuration,
                ),
            )
        }
    }
}
