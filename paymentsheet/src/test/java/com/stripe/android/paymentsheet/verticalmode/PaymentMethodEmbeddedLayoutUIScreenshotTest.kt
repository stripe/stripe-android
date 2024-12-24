package com.stripe.android.paymentsheet.verticalmode

import android.graphics.Color
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.toArgb
import com.stripe.android.model.PaymentMethodFixtures
import com.stripe.android.paymentelement.ExperimentalEmbeddedPaymentElementApi
import com.stripe.android.paymentsheet.DisplayableSavedPaymentMethod
import com.stripe.android.paymentsheet.PaymentSheet.Appearance.Embedded
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.screenshottesting.PaparazziRule
import com.stripe.android.uicore.StripeThemeDefaults
import com.stripe.android.utils.MockPaymentMethodsFactory
import com.stripe.android.utils.screenshots.PaymentSheetAppearance
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.mock
import kotlin.reflect.KClass

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
            TestPaymentMethodLayoutUi(
                rowStyle = getRowStyle(Embedded.RowStyle.FloatingButton::class)
            )
        }
    }

    @Test
    fun testFlatWitRadio() {
        paparazziRule.snapshot {
            TestPaymentMethodLayoutUi(
                rowStyle = getRowStyle(Embedded.RowStyle.FlatWithRadio::class)
            )
        }
    }

    @Test
    fun testFlatWithCheckmark() {
        paparazziRule.snapshot {
            TestPaymentMethodLayoutUi(
                rowStyle = getRowStyle(Embedded.RowStyle.FlatWithCheckmark::class)
            )
        }
    }

    @Test
    fun testSeparatorColorAndInsets() {
        paparazziRule.snapshot {
            TestPaymentMethodLayoutUi(
                rowStyle = getRowStyle(
                    type = Embedded.RowStyle.FlatWithCheckmark::class,
                    separatorThicknessDp = 5f,
                    separatorColor = Color.CYAN,
                    separatorInsetsDp = 40f
                )
            )
        }
    }

    @Test
    fun testBottomSeparatorOnly() {
        paparazziRule.snapshot {
            TestPaymentMethodLayoutUi(
                rowStyle = getRowStyle(
                    type = Embedded.RowStyle.FlatWithCheckmark::class,
                    topSeparatorEnabled = false,
                    bottomSeparatorEnabled = true
                )
            )
        }
    }

    @Test
    fun testTopSeparatorOnly() {
        paparazziRule.snapshot {
            TestPaymentMethodLayoutUi(
                rowStyle = getRowStyle(
                    type = Embedded.RowStyle.FlatWithCheckmark::class,
                    topSeparatorEnabled = true,
                    bottomSeparatorEnabled = false
                )
            )
        }
    }

    @Composable
    private fun TestPaymentMethodLayoutUi(
        rowStyle: Embedded.RowStyle
    ) {
        PaymentMethodEmbeddedLayoutUI(
            paymentMethods = paymentMethods,
            displayedSavedPaymentMethod = savedPaymentMethod,
            savedPaymentMethodAction =
            PaymentMethodVerticalLayoutInteractor.SavedPaymentMethodAction.MANAGE_ALL,
            selection = PaymentSelection.Saved(savedPaymentMethod.paymentMethod),
            isEnabled = true,
            onViewMorePaymentMethods = {},
            onSelectSavedPaymentMethod = {},
            onManageOneSavedPaymentMethod = {},
            imageLoader = mock(),
            rowStyle = rowStyle
        )
    }

    @Suppress("CyclomaticComplexMethod")
    @OptIn(ExperimentalEmbeddedPaymentElementApi::class)
    private fun <T : Embedded.RowStyle> getRowStyle(
        type: KClass<T>,
        separatorThicknessDp: Float? = null,
        separatorColor: Int? = null,
        separatorInsetsDp: Float? = null,
        topSeparatorEnabled: Boolean? = null,
        bottomSeparatorEnabled: Boolean? = null,
        selectedColor: Int? = null,
        unselectedColor: Int? = null,
        additionalInsetsDp: Float? = null,
        checkmarkColor: Int? = null,
        checkmarkInsetDp: Float? = null,
        spacingDp: Float? = null
    ): Embedded.RowStyle {
        return when (type) {
            Embedded.RowStyle.FlatWithRadio::class -> Embedded.RowStyle.FlatWithRadio(
                separatorThicknessDp = separatorThicknessDp ?: StripeThemeDefaults.flat.separatorThickness,
                separatorColor = separatorColor ?: StripeThemeDefaults.colorsLight.componentBorder.toArgb(),
                separatorInsetsDp = separatorInsetsDp ?: StripeThemeDefaults.flat.separatorInsets,
                topSeparatorEnabled = topSeparatorEnabled ?: StripeThemeDefaults.flat.topSeparatorEnabled,
                bottomSeparatorEnabled = bottomSeparatorEnabled ?: StripeThemeDefaults.flat.bottomSeparatorEnabled,
                selectedColor = selectedColor ?: StripeThemeDefaults.colorsLight.materialColors.primary.toArgb(),
                unselectedColor = unselectedColor ?: StripeThemeDefaults.colorsLight.componentBorder.toArgb(),
                additionalInsetsDp = additionalInsetsDp ?: StripeThemeDefaults.embeddedCommon.additionalInsetsDp
            )
            Embedded.RowStyle.FlatWithCheckmark::class -> Embedded.RowStyle.FlatWithCheckmark(
                separatorThicknessDp = separatorThicknessDp ?: StripeThemeDefaults.flat.separatorThickness,
                separatorColor = separatorColor ?: StripeThemeDefaults.colorsLight.componentBorder.toArgb(),
                separatorInsetsDp = separatorInsetsDp ?: StripeThemeDefaults.flat.separatorInsets,
                topSeparatorEnabled = topSeparatorEnabled ?: StripeThemeDefaults.flat.topSeparatorEnabled,
                bottomSeparatorEnabled = bottomSeparatorEnabled ?: StripeThemeDefaults.flat.bottomSeparatorEnabled,
                checkmarkColor = checkmarkColor ?: StripeThemeDefaults.colorsLight.materialColors.primary.toArgb(),
                checkmarkInsetDp = checkmarkInsetDp ?: StripeThemeDefaults.embeddedCommon.checkmarkInsetDp,
                additionalInsetsDp = additionalInsetsDp ?: StripeThemeDefaults.embeddedCommon.additionalInsetsDp
            )
            else -> Embedded.RowStyle.FloatingButton(
                spacingDp = spacingDp ?: StripeThemeDefaults.floating.spacing,
                additionalInsetsDp = additionalInsetsDp ?: StripeThemeDefaults.embeddedCommon.additionalInsetsDp
            )
        }
    }
}
