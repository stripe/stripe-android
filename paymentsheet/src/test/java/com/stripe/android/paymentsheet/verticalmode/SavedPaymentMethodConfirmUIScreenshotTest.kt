package com.stripe.android.paymentsheet.verticalmode

import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.stripe.android.core.strings.resolvableString
import com.stripe.android.model.CardBrand
import com.stripe.android.model.PaymentMethod
import com.stripe.android.paymentsheet.DisplayableSavedPaymentMethod
import com.stripe.android.screenshottesting.PaparazziRule
import org.junit.Rule
import kotlin.test.Test

internal class SavedPaymentMethodConfirmUIScreenshotTest {

    @get:Rule
    val paparazziRule = PaparazziRule(
        boxModifier = Modifier.padding(16.dp)
    )

    private val fakeSavedInteractor = object : SavedPaymentMethodConfirmInteractor {
        override val displayableSavedPaymentMethod = DisplayableSavedPaymentMethod.create(
            displayName = "···· 4242".resolvableString,
            paymentMethod = PaymentMethod(
                id = "pm_123",
                created = null,
                liveMode = false,
                code = PaymentMethod.Type.Card.code,
                type = PaymentMethod.Type.Card,
                card = PaymentMethod.Card(
                    brand = CardBrand.Visa,
                    last4 = "4242",
                )
            )
        )
        override val formElement = null
    }

    @Test
    fun testSavedPaymentMethodConfirmUI() {
        paparazziRule.snapshot {
            SavedPaymentMethodConfirmUI(
                savedPaymentMethodConfirmInteractor = fakeSavedInteractor
            )
        }
    }
}
