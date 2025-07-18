package com.stripe.android.paymentelement.embedded.content

import com.stripe.android.core.strings.resolvableString
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadataFactory
import com.stripe.android.paymentsheet.PaymentSheet.Appearance.Embedded
import com.stripe.android.paymentsheet.verticalmode.FakePaymentMethodVerticalLayoutInteractor
import com.stripe.android.screenshottesting.PaparazziRule
import com.stripe.android.screenshottesting.SystemAppearance
import org.junit.Rule
import kotlin.test.Test

internal class EmbeddedContentScreenshotTest {
    @get:Rule
    val paparazziRule = PaparazziRule(
        SystemAppearance.entries,
        includeStripeTheme = false,
    )

    @Test
    fun displaysVerticalModeList() {
        val metadata = PaymentMethodMetadataFactory.create()
        val interactor = FakePaymentMethodVerticalLayoutInteractor.create(metadata)
        val content = EmbeddedContent(
            interactor = interactor,
            embeddedViewDisplaysMandateText = true,
            appearance = Embedded(Embedded.RowStyle.FloatingButton.default),
            isImmediateAction = false,
        )
        paparazziRule.snapshot {
            content.Content()
        }
    }

    @Test
    fun displaysVerticalModeListWithMandate() {
        val metadata = PaymentMethodMetadataFactory.create()
        val interactor = FakePaymentMethodVerticalLayoutInteractor.create(
            paymentMethodMetadata = metadata,
            mandate = "Some mandate".resolvableString,
        )
        val content = EmbeddedContent(
            interactor = interactor,
            embeddedViewDisplaysMandateText = true,
            appearance = Embedded(Embedded.RowStyle.FloatingButton.default),
            isImmediateAction = false,
        )
        paparazziRule.snapshot {
            content.Content()
        }
    }

    @Test
    fun displaysVerticalModeListWithoutMandate() {
        val metadata = PaymentMethodMetadataFactory.create()
        val interactor = FakePaymentMethodVerticalLayoutInteractor.create(
            paymentMethodMetadata = metadata,
            mandate = "Some mandate".resolvableString,
        )
        val content = EmbeddedContent(
            interactor = interactor,
            embeddedViewDisplaysMandateText = false,
            appearance = Embedded(Embedded.RowStyle.FloatingButton.default),
            isImmediateAction = false,
        )
        paparazziRule.snapshot {
            content.Content()
        }
    }
}
