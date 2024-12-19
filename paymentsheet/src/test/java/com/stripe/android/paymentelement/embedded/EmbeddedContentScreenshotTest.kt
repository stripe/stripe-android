package com.stripe.android.paymentelement.embedded

import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.stripe.android.core.strings.resolvableString
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadataFactory
import com.stripe.android.paymentsheet.verticalmode.FakePaymentMethodVerticalLayoutInteractor
import com.stripe.android.screenshottesting.PaparazziRule
import org.junit.Rule
import kotlin.test.Test

internal class EmbeddedContentScreenshotTest {
    @get:Rule
    val paparazziRule = PaparazziRule(boxModifier = Modifier.padding(horizontal = 20.dp))

    @Test
    fun displaysVerticalModeList() {
        val metadata = PaymentMethodMetadataFactory.create()
        val interactor = FakePaymentMethodVerticalLayoutInteractor.create(metadata)
        val content = EmbeddedContent(interactor)
        paparazziRule.snapshot {
            content.Content()
        }
    }

    @Test
    fun displaysVerticalModeListWithMandate() {
        val metadata = PaymentMethodMetadataFactory.create()
        val interactor = FakePaymentMethodVerticalLayoutInteractor.create(metadata)
        val content = EmbeddedContent(interactor, mandate = "Some mandate".resolvableString)
        paparazziRule.snapshot {
            content.Content()
        }
    }
}
