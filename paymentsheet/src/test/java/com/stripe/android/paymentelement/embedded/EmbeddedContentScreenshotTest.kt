package com.stripe.android.paymentelement.embedded

import com.stripe.android.core.strings.resolvableString
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadataFactory
import com.stripe.android.paymentsheet.verticalmode.FakePaymentMethodVerticalLayoutInteractor
import com.stripe.android.screenshottesting.PaparazziRule
import org.junit.Rule
import kotlin.test.Test

internal class EmbeddedContentScreenshotTest {
    @get:Rule
    val paparazziRule = PaparazziRule()

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
    fun displaysVerticalModeListWithError() {
        val metadata = PaymentMethodMetadataFactory.create()
        val interactor = FakePaymentMethodVerticalLayoutInteractor.create(metadata)
        val content = EmbeddedContent(interactor, error = "Some error".resolvableString)
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

    @Test
    fun displaysVerticalModeListWithErrorAndMandate() {
        val metadata = PaymentMethodMetadataFactory.create()
        val interactor = FakePaymentMethodVerticalLayoutInteractor.create(metadata)
        val content = EmbeddedContent(
            interactor = interactor,
            error = "Some error".resolvableString,
            mandate = "Some mandate".resolvableString
        )
        paparazziRule.snapshot {
            content.Content()
        }
    }
}
