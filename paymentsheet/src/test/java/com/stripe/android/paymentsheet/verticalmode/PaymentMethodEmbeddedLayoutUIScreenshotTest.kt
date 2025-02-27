package com.stripe.android.paymentsheet.verticalmode

import android.graphics.Color
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.stripe.android.model.PaymentMethodFixtures
import com.stripe.android.paymentelement.ExperimentalEmbeddedPaymentElementApi
import com.stripe.android.paymentsheet.DisplayableSavedPaymentMethod
import com.stripe.android.paymentsheet.PaymentSheet.Appearance.Embedded
import com.stripe.android.paymentsheet.PaymentSheet.Appearance.Embedded.RowStyle.FlatWithCheckmark
import com.stripe.android.paymentsheet.PaymentSheet.Appearance.Embedded.RowStyle.FlatWithRadio
import com.stripe.android.paymentsheet.PaymentSheet.Appearance.Embedded.RowStyle.FloatingButton
import com.stripe.android.screenshottesting.PaparazziRule
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
                rowStyle = getRowStyle(FloatingButton::class)
            )
        }
    }

    @Test
    fun testFlatWitRadio() {
        paparazziRule.snapshot {
            TestPaymentMethodLayoutUi(
                rowStyle = getRowStyle(FlatWithRadio::class)
            )
        }
    }

    @Test
    fun testFlatWithCheckmark() {
        paparazziRule.snapshot {
            TestPaymentMethodLayoutUi(
                rowStyle = getRowStyle(FlatWithCheckmark::class)
            )
        }
    }

    @Test
    fun testSeparatorColorAndInsets() {
        paparazziRule.snapshot {
            TestPaymentMethodLayoutUi(
                rowStyle = getRowStyle(
                    type = FlatWithCheckmark::class,
                    separatorThicknessDp = 5f,
                    separatorColor = Color.CYAN,
                    startSeparatorInset = 40f,
                    endSeparatorInset = 40f,
                )
            )
        }
    }

    @Test
    fun testBottomSeparatorOnly() {
        paparazziRule.snapshot {
            TestPaymentMethodLayoutUi(
                rowStyle = getRowStyle(
                    type = FlatWithCheckmark::class,
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
                    type = FlatWithCheckmark::class,
                    topSeparatorEnabled = true,
                    bottomSeparatorEnabled = false
                )
            )
        }
    }

    @Test
    fun testSavedPaymentMethodOnly() {
        paparazziRule.snapshot {
            TestPaymentMethodLayoutUi(
                rowStyle = getRowStyle(
                    type = FlatWithCheckmark::class,
                    topSeparatorEnabled = true,
                    bottomSeparatorEnabled = true,
                    separatorThicknessDp = 5f
                ),
                newPaymentMethods = listOf()
            )
        }
    }

    @Test
    fun testNewPaymentMethodsOnly() {
        paparazziRule.snapshot {
            TestPaymentMethodLayoutUi(
                rowStyle = getRowStyle(
                    type = FlatWithCheckmark::class,
                    topSeparatorEnabled = true,
                    bottomSeparatorEnabled = true,
                    separatorThicknessDp = 5f
                ),
                displayableSavedPaymentMethod = null
            )
        }
    }

    @Composable
    private fun TestPaymentMethodLayoutUi(
        rowStyle: Embedded.RowStyle,
        displayableSavedPaymentMethod: DisplayableSavedPaymentMethod? = savedPaymentMethod,
        newPaymentMethods: List<DisplayablePaymentMethod> = paymentMethods
    ) {
        val scrollState = rememberScrollState()
        PaymentMethodEmbeddedLayoutUI(
            paymentMethods = newPaymentMethods,
            displayedSavedPaymentMethod = displayableSavedPaymentMethod,
            savedPaymentMethodAction =
            PaymentMethodVerticalLayoutInteractor.SavedPaymentMethodAction.MANAGE_ALL,
            selection = PaymentMethodVerticalLayoutInteractor.Selection.Saved,
            isEnabled = true,
            onViewMorePaymentMethods = {},
            onSelectSavedPaymentMethod = {},
            onManageOneSavedPaymentMethod = {},
            imageLoader = mock(),
            rowStyle = rowStyle,
            modifier = Modifier.verticalScroll(scrollState),
        )
    }

    @Suppress("CyclomaticComplexMethod")
    @OptIn(ExperimentalEmbeddedPaymentElementApi::class)
    private fun <T : Embedded.RowStyle> getRowStyle(
        type: KClass<T>,
        separatorThicknessDp: Float? = null,
        separatorColor: Int? = null,
        startSeparatorInset: Float? = null,
        endSeparatorInset: Float? = null,
        topSeparatorEnabled: Boolean? = null,
        bottomSeparatorEnabled: Boolean? = null,
        selectedColor: Int? = null,
        unselectedColor: Int? = null,
        additionalVerticalInsetsDp: Float? = null,
        horizontalInsetsDp: Float? = null,
        checkmarkColor: Int? = null,
        checkmarkInsetDp: Float? = null,
        spacingDp: Float? = null
    ): Embedded.RowStyle {
        return when (type) {
            FlatWithRadio::class -> FlatWithRadio(
                separatorThicknessDp = separatorThicknessDp ?: FlatWithRadio.defaultLight.separatorThicknessDp,
                separatorColor = separatorColor ?: FlatWithRadio.defaultLight.separatorColor,
                startSeparatorInsetDp = startSeparatorInset ?: FlatWithRadio.defaultLight.startSeparatorInsetDp,
                endSeparatorInsetDp = endSeparatorInset ?: FlatWithRadio.defaultLight.endSeparatorInsetDp,
                topSeparatorEnabled = topSeparatorEnabled ?: FlatWithRadio.defaultLight.topSeparatorEnabled,
                bottomSeparatorEnabled = bottomSeparatorEnabled ?: FlatWithRadio.defaultLight.bottomSeparatorEnabled,
                selectedColor = selectedColor ?: FlatWithRadio.defaultLight.selectedColor,
                unselectedColor = unselectedColor ?: FlatWithRadio.defaultLight.unselectedColor,
                additionalVerticalInsetsDp = additionalVerticalInsetsDp
                    ?: FlatWithRadio.defaultLight.additionalVerticalInsetsDp,
                horizontalInsetsDp = horizontalInsetsDp ?: FlatWithRadio.defaultLight.horizontalInsetsDp
            )
            FlatWithCheckmark::class -> FlatWithCheckmark(
                separatorThicknessDp = separatorThicknessDp ?: FlatWithCheckmark.defaultLight.separatorThicknessDp,
                separatorColor = separatorColor ?: FlatWithCheckmark.defaultLight.separatorColor,
                startSeparatorInsetDp = startSeparatorInset ?: FlatWithCheckmark.defaultLight.startSeparatorInsetDp,
                endSeparatorInsetDp = endSeparatorInset ?: FlatWithCheckmark.defaultLight.endSeparatorInsetDp,
                topSeparatorEnabled = topSeparatorEnabled ?: FlatWithCheckmark.defaultLight.topSeparatorEnabled,
                bottomSeparatorEnabled = bottomSeparatorEnabled
                    ?: FlatWithCheckmark.defaultLight.bottomSeparatorEnabled,
                checkmarkColor = checkmarkColor ?: FlatWithCheckmark.defaultLight.checkmarkColor,
                checkmarkInsetDp = checkmarkInsetDp ?: FlatWithCheckmark.defaultLight.checkmarkInsetDp,
                additionalVerticalInsetsDp = additionalVerticalInsetsDp
                    ?: FlatWithCheckmark.defaultLight.additionalVerticalInsetsDp,
                horizontalInsetsDp = horizontalInsetsDp ?: FlatWithCheckmark.defaultLight.horizontalInsetsDp
            )
            else -> FloatingButton(
                spacingDp = spacingDp ?: FloatingButton.default.spacingDp,
                additionalInsetsDp = additionalVerticalInsetsDp ?: FloatingButton.default.additionalInsetsDp
            )
        }
    }
}
