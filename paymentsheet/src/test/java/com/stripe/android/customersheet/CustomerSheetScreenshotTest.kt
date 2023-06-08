package com.stripe.android.customersheet

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.stripe.android.customersheet.ui.CustomerSheetScreen
import com.stripe.android.model.CardBrand
import com.stripe.android.model.PaymentMethod
import com.stripe.android.paymentsheet.PaymentOptionsItem
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
                    title = "Screenshot testing",
                    paymentMethods = listOf(),
                    selectedPaymentMethodId = null,
                    isLiveMode = false,
                    isProcessing = false,
                    isEditing = false,
                ),
            )
        }
    }

    @Test
    fun testWithPaymentMethods() {
        paparazzi.snapshot {
            CustomerSheetScreen(
                viewState = CustomerSheetViewState.SelectPaymentMethod(
                    title = "Screenshot testing",
                    paymentMethods = List(5) {
                        PaymentOptionsItem.SavedPaymentMethod(
                            displayName = "424$it",
                            paymentMethod = PaymentMethod(
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
                        )
                    },
                    selectedPaymentMethodId = "pm_1230",
                    isLiveMode = false,
                    isProcessing = false,
                    isEditing = false,
                    errorMessage = "This is an error message."
                ),
            )
        }
    }
}
