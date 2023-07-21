package com.stripe.android.customersheet

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.stripe.android.customersheet.ui.CustomerSheetScreen
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

    @Test
    fun testDefault() {
        paparazzi.snapshot {
            CustomerSheetScreen(
                viewState = CustomerSheetViewState.SelectPaymentMethod(
                    title = null,
                    savedPaymentMethods = emptyList(),
                    paymentSelection = null,
                    isLiveMode = false,
                    isProcessing = false,
                    isEditing = false,
                    isGooglePayEnabled = false,
                    primaryButtonVisible = true,
                    primaryButtonLabel = null,
                ),
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
                viewState = CustomerSheetViewState.SelectPaymentMethod(
                    title = "Screenshot testing",
                    savedPaymentMethods = savedPaymentMethods,
                    paymentSelection = PaymentSelection.Saved(
                        savedPaymentMethods.first()
                    ),
                    isLiveMode = false,
                    isProcessing = false,
                    isEditing = false,
                    isGooglePayEnabled = true,
                    primaryButtonVisible = true,
                    primaryButtonLabel = "Continue",
                    errorMessage = "This is an error message.",
                ),
                paymentMethodNameProvider = {
                    counter++
                    "424$counter"
                }
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
                viewState = CustomerSheetViewState.SelectPaymentMethod(
                    title = "Screenshot testing",
                    savedPaymentMethods = savedPaymentMethods,
                    paymentSelection = PaymentSelection.Saved(
                        savedPaymentMethods.first()
                    ),
                    isLiveMode = false,
                    isProcessing = false,
                    isEditing = true,
                    isGooglePayEnabled = true,
                    primaryButtonVisible = false,
                    primaryButtonLabel = "Continue",
                    errorMessage = "This is an error message.",
                ),
                paymentMethodNameProvider = {
                    counter++
                    "424$counter"
                }
            )
        }
    }

    @Test
    fun testGooglePay() {
        paparazzi.snapshot {
            CustomerSheetScreen(
                viewState = CustomerSheetViewState.SelectPaymentMethod(
                    title = "Screenshot testing",
                    savedPaymentMethods = emptyList(),
                    paymentSelection = PaymentSelection.GooglePay,
                    isLiveMode = false,
                    isProcessing = false,
                    isEditing = false,
                    isGooglePayEnabled = true,
                    primaryButtonVisible = true,
                    primaryButtonLabel = "Continue",
                    errorMessage = "This is an error message.",
                ),
                paymentMethodNameProvider = { it!! }
            )
        }
    }

    @Test
    fun testAddPaymentMethodDisabled() {
        paparazzi.snapshot {
            CustomerSheetScreen(
                viewState = CustomerSheetViewState.AddPaymentMethod(
                    paymentMethodCode = PaymentMethod.Type.Card.code,
                    formViewData = FormViewModel.ViewData(),
                    enabled = true,
                    isLiveMode = false,
                    isProcessing = false,
                    errorMessage = "This is an error message.",
                ),
                paymentMethodNameProvider = { it!! }
            )
        }
    }

    @Test
    fun testAddPaymentMethodEnabled() {
        paparazzi.snapshot {
            CustomerSheetScreen(
                viewState = CustomerSheetViewState.AddPaymentMethod(
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
                    enabled = true,
                    isLiveMode = false,
                    isProcessing = false,
                ),
                paymentMethodNameProvider = { it!! }
            )
        }
    }
}
