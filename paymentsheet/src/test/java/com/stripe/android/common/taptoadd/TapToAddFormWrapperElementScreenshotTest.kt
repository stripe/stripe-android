package com.stripe.android.common.taptoadd

import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.stripe.android.core.strings.resolvableString
import com.stripe.android.model.CardBrand
import com.stripe.android.model.PaymentMethod
import com.stripe.android.paymentsheet.DisplayableSavedPaymentMethod
import com.stripe.android.screenshottesting.PaparazziRule
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Rule
import kotlin.test.Test

internal class TapToAddFormWrapperElementScreenshotTest {
    @get:Rule
    val paparazziRule = PaparazziRule(
        boxModifier = Modifier
            .padding(16.dp)
    )

    private val savedVisa = DisplayableSavedPaymentMethod.create(
        displayName = "···· 4242".resolvableString,
        paymentMethod = PaymentMethod(
            id = "pm_001",
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

    @Test
    fun testWithCollectedPaymentMethod() {
        val collectedPaymentMethodFlow = MutableStateFlow(savedVisa)
        val tapToAddHelper = FakeTapToAddHelper.noOp(collectedPaymentMethod = collectedPaymentMethodFlow)
        val element = TapToAddFormWrapperElement(
            tapToAddHelper = tapToAddHelper,
            elements = emptyList(),
        )

        paparazziRule.snapshot {
            element.ComposeUI(
                enabled = true,
                hiddenIdentifiers = emptySet(),
                lastTextFieldIdentifier = null,
            )
        }
    }
}
