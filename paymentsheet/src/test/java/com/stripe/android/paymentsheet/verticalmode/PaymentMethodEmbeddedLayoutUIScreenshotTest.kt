package com.stripe.android.paymentsheet.verticalmode

import android.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.stripe.android.model.PaymentMethodFixtures
import com.stripe.android.paymentelement.ExperimentalEmbeddedPaymentElementApi
import com.stripe.android.paymentsheet.DisplayableSavedPaymentMethod
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.screenshottesting.PaparazziRule
import com.stripe.android.uicore.StripeThemeDefaults
import com.stripe.android.utils.MockPaymentMethodsFactory
import com.stripe.android.utils.screenshots.PaymentSheetAppearance
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.mock

@OptIn(ExperimentalEmbeddedPaymentElementApi::class)
class PaymentMethodEmbeddedLayoutUIScreenshotTest {
    @get:Rule
    val paparazziRule = PaparazziRule(PaymentSheetAppearance.entries)

    private val paymentMethods: List<DisplayablePaymentMethod> by lazy {
        MockPaymentMethodsFactory.create().map {
            it.asDisplayablePaymentMethod(
                customerSavedPaymentMethods = emptyList(),
                incentive = null,
                onClick = {},
            )
        }
    }

    private val savedPaymentMethod: DisplayableSavedPaymentMethod = PaymentMethodFixtures.displayableCard()

    @Test
    fun testFloatingButton() {
        paparazziRule.snapshot {
            PaymentMethodEmbeddedLayoutUI(
                paymentMethods = paymentMethods,
                displayedSavedPaymentMethod = savedPaymentMethod,
                savedPaymentMethodAction =
                PaymentMethodVerticalLayoutInteractor.SavedPaymentMethodAction.MANAGE_ALL,
                selection = PaymentSelection.Saved(savedPaymentMethod.paymentMethod),
                isEnabled = true,
                onEditPaymentMethod = {},
                onViewMorePaymentMethods = {},
                onSelectSavedPaymentMethod = {},
                onManageOneSavedPaymentMethod = {},
                imageLoader = mock(),
                rowStyle = PaymentSheet.Appearance.Embedded.RowStyle.FloatingButton.default
            )
        }
    }

    @Test
    fun testFlatWitRadio() {
        paparazziRule.snapshot {
            PaymentMethodEmbeddedLayoutUI(
                paymentMethods = paymentMethods,
                displayedSavedPaymentMethod = savedPaymentMethod,
                savedPaymentMethodAction =
                PaymentMethodVerticalLayoutInteractor.SavedPaymentMethodAction.MANAGE_ALL,
                selection = PaymentSelection.Saved(savedPaymentMethod.paymentMethod),
                isEnabled = true,
                onEditPaymentMethod = {},
                onViewMorePaymentMethods = {},
                onSelectSavedPaymentMethod = {},
                onManageOneSavedPaymentMethod = {},
                imageLoader = mock(),
                rowStyle = PaymentSheet.Appearance.Embedded.RowStyle.FlatWithRadio.defaultLight
            )
        }
    }

    @Test
    fun testFlatWithCheckmark() {
        paparazziRule.snapshot {
            PaymentMethodEmbeddedLayoutUI(
                paymentMethods = paymentMethods,
                displayedSavedPaymentMethod = savedPaymentMethod,
                savedPaymentMethodAction =
                PaymentMethodVerticalLayoutInteractor.SavedPaymentMethodAction.MANAGE_ALL,
                selection = PaymentSelection.Saved(savedPaymentMethod.paymentMethod),
                isEnabled = true,
                onEditPaymentMethod = {},
                onViewMorePaymentMethods = {},
                onSelectSavedPaymentMethod = {},
                onManageOneSavedPaymentMethod = {},
                imageLoader = mock(),
                rowStyle = PaymentSheet.Appearance.Embedded.RowStyle.FlatWithCheckmark.defaultLight
            )
        }
    }

    @Test
    fun testSeparatorColorAndInsets() {
        paparazziRule.snapshot {
            PaymentMethodEmbeddedLayoutUI(
                paymentMethods = paymentMethods,
                displayedSavedPaymentMethod = savedPaymentMethod,
                savedPaymentMethodAction =
                PaymentMethodVerticalLayoutInteractor.SavedPaymentMethodAction.MANAGE_ALL,
                selection = PaymentSelection.Saved(savedPaymentMethod.paymentMethod),
                isEnabled = true,
                onEditPaymentMethod = {},
                onViewMorePaymentMethods = {},
                onSelectSavedPaymentMethod = {},
                onManageOneSavedPaymentMethod = {},
                imageLoader = mock(),
                rowStyle = PaymentSheet.Appearance.Embedded.RowStyle.FlatWithCheckmark(
                    separatorThicknessDp = 5f,
                    separatorColor = Color.CYAN,
                    separatorInsetsDp = 40f,
                    topSeparatorEnabled = true,
                    bottomSeparatorEnabled = true,
                    checkmarkColor = StripeThemeDefaults.colorsLight.materialColors.primary.toArgb(),
                    checkmarkInsetDp = StripeThemeDefaults.embeddedCommon.checkmarkInsetDp,
                    additionalInsetsDp = StripeThemeDefaults.embeddedCommon.additionalInsetsDp
                )
            )
        }
    }

    @Test
    fun testBottomSeparatorOnly() {
        paparazziRule.snapshot {
            PaymentMethodEmbeddedLayoutUI(
                paymentMethods = paymentMethods,
                displayedSavedPaymentMethod = savedPaymentMethod,
                savedPaymentMethodAction =
                PaymentMethodVerticalLayoutInteractor.SavedPaymentMethodAction.MANAGE_ALL,
                selection = PaymentSelection.Saved(savedPaymentMethod.paymentMethod),
                isEnabled = true,
                onEditPaymentMethod = {},
                onViewMorePaymentMethods = {},
                onSelectSavedPaymentMethod = {},
                onManageOneSavedPaymentMethod = {},
                imageLoader = mock(),
                rowStyle = PaymentSheet.Appearance.Embedded.RowStyle.FlatWithCheckmark(
                    separatorThicknessDp = StripeThemeDefaults.flat.separatorThickness,
                    separatorColor = StripeThemeDefaults.colorsLight.componentBorder.toArgb(),
                    separatorInsetsDp = StripeThemeDefaults.flat.separatorInsets,
                    topSeparatorEnabled = false,
                    bottomSeparatorEnabled = true,
                    checkmarkColor = StripeThemeDefaults.colorsLight.materialColors.primary.toArgb(),
                    checkmarkInsetDp = StripeThemeDefaults.embeddedCommon.checkmarkInsetDp,
                    additionalInsetsDp = StripeThemeDefaults.embeddedCommon.additionalInsetsDp
                )
            )
        }
    }

    @Test
    fun testTopSeparatorOnly() {
        paparazziRule.snapshot {
            PaymentMethodEmbeddedLayoutUI(
                paymentMethods = paymentMethods,
                displayedSavedPaymentMethod = savedPaymentMethod,
                savedPaymentMethodAction =
                PaymentMethodVerticalLayoutInteractor.SavedPaymentMethodAction.MANAGE_ALL,
                selection = PaymentSelection.Saved(savedPaymentMethod.paymentMethod),
                isEnabled = true,
                onEditPaymentMethod = {},
                onViewMorePaymentMethods = {},
                onSelectSavedPaymentMethod = {},
                onManageOneSavedPaymentMethod = {},
                imageLoader = mock(),
                rowStyle = PaymentSheet.Appearance.Embedded.RowStyle.FlatWithCheckmark(
                    separatorThicknessDp = StripeThemeDefaults.flat.separatorThickness,
                    separatorColor = StripeThemeDefaults.colorsLight.componentBorder.toArgb(),
                    separatorInsetsDp = StripeThemeDefaults.flat.separatorInsets,
                    topSeparatorEnabled = true,
                    bottomSeparatorEnabled = false,
                    checkmarkColor = StripeThemeDefaults.colorsLight.materialColors.primary.toArgb(),
                    checkmarkInsetDp = StripeThemeDefaults.embeddedCommon.checkmarkInsetDp,
                    additionalInsetsDp = StripeThemeDefaults.embeddedCommon.additionalInsetsDp
                )
            )
        }
    }
}
