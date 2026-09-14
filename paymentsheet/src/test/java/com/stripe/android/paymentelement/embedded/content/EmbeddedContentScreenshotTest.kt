package com.stripe.android.paymentelement.embedded.content

import com.stripe.android.core.strings.resolvableString
import com.stripe.android.link.TestFactory
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadataFactory
import com.stripe.android.paymentelement.AppearanceAPIAdditionsPreview
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.PaymentSheet.Appearance.Embedded
import com.stripe.android.paymentsheet.state.LinkState
import com.stripe.android.paymentsheet.verticalmode.FakePaymentMethodVerticalLayoutInteractor
import com.stripe.android.screenshottesting.PaparazziRule
import com.stripe.android.screenshottesting.SystemAppearance
import com.stripe.android.utils.screenshots.PaymentSheetAppearance
import org.junit.Rule
import kotlin.test.Test

@OptIn(AppearanceAPIAdditionsPreview::class)
internal class EmbeddedContentScreenshotTest {
    @get:Rule
    val paparazziRule = PaparazziRule(
        SystemAppearance.entries,
        includeStripeTheme = false,
    )

    @Test
    fun displaysVerticalModeList() {
        val metadata = createMetadata()
        val interactor = FakePaymentMethodVerticalLayoutInteractor.create(metadata)
        val content = EmbeddedContent(
            interactor = interactor,
            embeddedViewDisplaysMandateText = true,
            appearance = PaymentSheet.Appearance(
                embeddedAppearance = Embedded(Embedded.RowStyle.FloatingButton.default),
            ),
            isImmediateAction = false,
        )
        paparazziRule.snapshot {
            content.Content()
        }
    }

    @Test
    fun displaysVerticalModeListWithMandate() {
        val metadata = createMetadata()
        val interactor = FakePaymentMethodVerticalLayoutInteractor.create(
            paymentMethodMetadata = metadata,
            mandate = "Some mandate".resolvableString,
        )
        val content = EmbeddedContent(
            interactor = interactor,
            embeddedViewDisplaysMandateText = true,
            appearance = PaymentSheet.Appearance(
                embeddedAppearance = Embedded(Embedded.RowStyle.FloatingButton.default),
            ),
            isImmediateAction = false,
        )
        paparazziRule.snapshot {
            content.Content()
        }
    }

    @Test
    fun displaysVerticalModeListWithoutMandate() {
        val metadata = createMetadata()
        val interactor = FakePaymentMethodVerticalLayoutInteractor.create(
            paymentMethodMetadata = metadata,
            mandate = "Some mandate".resolvableString,
        )
        val content = EmbeddedContent(
            interactor = interactor,
            embeddedViewDisplaysMandateText = false,
            appearance = PaymentSheet.Appearance(
                embeddedAppearance = Embedded(Embedded.RowStyle.FloatingButton.default),
            ),
            isImmediateAction = false,
        )
        paparazziRule.snapshot {
            content.Content()
        }
    }

    @Test
    fun displaysAlwaysDarkTheme() {
        snapshotWithAppearance(
            PaymentSheet.Appearance(
                themeMode = PaymentSheet.ThemeMode.AlwaysDark,
                embeddedAppearance = Embedded(Embedded.RowStyle.FloatingButton.default),
            ),
        )
    }

    @Test
    fun displaysAlwaysLightTheme() {
        snapshotWithAppearance(
            PaymentSheet.Appearance(
                themeMode = PaymentSheet.ThemeMode.AlwaysLight,
                embeddedAppearance = Embedded(Embedded.RowStyle.FloatingButton.default),
            ),
        )
    }

    @Test
    fun displaysCustomAppearanceTheme() {
        val customAppearance = PaymentSheetAppearance.CrazyAppearance.appearance
        snapshotWithAppearance(
            PaymentSheet.Appearance(
                colorsLight = customAppearance.colorsLight,
                colorsDark = customAppearance.colorsDark,
                shapes = customAppearance.shapes,
                typography = customAppearance.typography,
                embeddedAppearance = Embedded(Embedded.RowStyle.FloatingButton.default),
            )
        )
    }

    private fun snapshotWithAppearance(appearance: PaymentSheet.Appearance) {
        val metadata = createMetadata()
        val interactor = FakePaymentMethodVerticalLayoutInteractor.create(metadata)
        val content = EmbeddedContent(
            interactor = interactor,
            embeddedViewDisplaysMandateText = true,
            appearance = appearance,
            isImmediateAction = false,
        )
        paparazziRule.snapshot {
            content.Content()
        }
    }

    private fun createMetadata(): PaymentMethodMetadata {
        return PaymentMethodMetadataFactory.create(
            linkState = LinkState(
                configuration = TestFactory.LINK_CONFIGURATION_WITH_INSTANT_DEBITS_ONBOARDING,
                loginState = LinkState.LoginState.LoggedOut,
                signupMode = null,
            ),
        )
    }
}
