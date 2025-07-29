package com.stripe.android.paymentsheet.verticalmode

import android.graphics.Color
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.stripe.android.model.PaymentMethodFixtures
import com.stripe.android.paymentsheet.DisplayableSavedPaymentMethod
import com.stripe.android.paymentsheet.PaymentSheet.Appearance.Embedded
import com.stripe.android.paymentsheet.PaymentSheet.Appearance.Embedded.RowStyle.FlatWithCheckmark
import com.stripe.android.paymentsheet.PaymentSheet.Appearance.Embedded.RowStyle.FlatWithDisclosure
import com.stripe.android.paymentsheet.PaymentSheet.Appearance.Embedded.RowStyle.FlatWithRadio
import com.stripe.android.paymentsheet.PaymentSheet.Appearance.Embedded.RowStyle.FloatingButton
import com.stripe.android.screenshottesting.PaparazziRule
import com.stripe.android.utils.MockPaymentMethodsFactory
import com.stripe.android.utils.screenshots.PaymentSheetAppearance
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.mock
import kotlin.reflect.KClass

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
                appearance = getEmbeddedAppearance(FloatingButton::class)
            )
        }
    }

    @Test
    fun testFlatWithRadio() {
        paparazziRule.snapshot {
            TestPaymentMethodLayoutUi(
                appearance = getEmbeddedAppearance(FlatWithRadio::class)
            )
        }
    }

    @Test
    fun testFlatWithCheckmark() {
        paparazziRule.snapshot {
            TestPaymentMethodLayoutUi(
                appearance = getEmbeddedAppearance(FlatWithCheckmark::class)
            )
        }
    }

    @Test
    fun testFlatWithDisclosure() {
        paparazziRule.snapshot {
            TestPaymentMethodLayoutUi(
                appearance = getEmbeddedAppearance(FlatWithDisclosure::class)
            )
        }
    }

    @Test
    fun testSeparatorColorAndInsets() {
        paparazziRule.snapshot {
            TestPaymentMethodLayoutUi(
                appearance = getEmbeddedAppearance(
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
                appearance = getEmbeddedAppearance(
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
                appearance = getEmbeddedAppearance(
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
                appearance = getEmbeddedAppearance(
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
                appearance = getEmbeddedAppearance(
                    type = FlatWithCheckmark::class,
                    topSeparatorEnabled = true,
                    bottomSeparatorEnabled = true,
                    separatorThicknessDp = 5f
                ),
                displayableSavedPaymentMethod = null
            )
        }
    }

    @Test
    fun testNewPaymentMethodsWithSelection() {
        paparazziRule.snapshot {
            TestPaymentMethodLayoutUiWithNewSelection(
                rowStyle = FlatWithCheckmark.default,
                selection = PaymentMethodVerticalLayoutInteractor.Selection.New(
                    code = "card",
                    changeDetails = "Visa **** 4242",
                    canBeChanged = true
                )
            )
        }
    }

    @Test
    fun testNewPaymentMethodsWithSelectionAndDisclosure() {
        paparazziRule.snapshot {
            TestPaymentMethodLayoutUiWithNewSelection(
                rowStyle = FlatWithDisclosure.default,
                selection = PaymentMethodVerticalLayoutInteractor.Selection.New(
                    code = "card",
                    changeDetails = "Visa **** 4242",
                    canBeChanged = true
                )
            )
        }
    }

    @Test
    fun testNewPaymentMethodsWithSelectionWithoutChangeDetails() {
        paparazziRule.snapshot {
            TestPaymentMethodLayoutUiWithNewSelection(
                rowStyle = FlatWithCheckmark.default,
                selection = PaymentMethodVerticalLayoutInteractor.Selection.New(
                    code = "klarna",
                    changeDetails = null,
                    canBeChanged = true
                )
            )
        }
    }

    @Test
    fun testWithSavedPaymentMethodsSetHeightAndCanManageAll() {
        paparazziRule.snapshot {
            TestPaymentMethodLayoutUiWithSetHeight(
                action = PaymentMethodVerticalLayoutInteractor.SavedPaymentMethodAction.MANAGE_ALL,
            )
        }
    }

    @Test
    fun testWithSavedPaymentMethodsSetHeightAndCanManageOnlyOne() {
        paparazziRule.snapshot {
            TestPaymentMethodLayoutUiWithSetHeight(
                action = PaymentMethodVerticalLayoutInteractor.SavedPaymentMethodAction.MANAGE_ONE,
            )
        }
    }

    @Composable
    private fun TestPaymentMethodLayoutUi(
        appearance: Embedded,
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
            appearance = appearance,
            modifier = Modifier.verticalScroll(scrollState),
        )
    }

    @Composable
    private fun TestPaymentMethodLayoutUiWithNewSelection(
        rowStyle: Embedded.RowStyle,
        selection: PaymentMethodVerticalLayoutInteractor.Selection? = null,
        newPaymentMethods: List<DisplayablePaymentMethod> = paymentMethods
    ) {
        val scrollState = rememberScrollState()
        PaymentMethodEmbeddedLayoutUI(
            paymentMethods = newPaymentMethods,
            displayedSavedPaymentMethod = null,
            savedPaymentMethodAction =
            PaymentMethodVerticalLayoutInteractor.SavedPaymentMethodAction.MANAGE_ALL,
            selection = selection,
            isEnabled = true,
            onViewMorePaymentMethods = {},
            onSelectSavedPaymentMethod = {},
            onManageOneSavedPaymentMethod = {},
            imageLoader = mock(),
            appearance = Embedded(rowStyle),
            modifier = Modifier.verticalScroll(scrollState),
        )
    }

    @Composable
    private fun TestPaymentMethodLayoutUiWithSetHeight(
        action: PaymentMethodVerticalLayoutInteractor.SavedPaymentMethodAction,
    ) {
        Box(
            modifier = Modifier.requiredHeight(450.dp)
        ) {
            PaymentMethodEmbeddedLayoutUI(
                paymentMethods = emptyList(),
                displayedSavedPaymentMethod = savedPaymentMethod,
                savedPaymentMethodAction = action,
                selection = null,
                isEnabled = true,
                onViewMorePaymentMethods = {},
                onSelectSavedPaymentMethod = {},
                onManageOneSavedPaymentMethod = {},
                imageLoader = mock(),
                appearance = getEmbeddedAppearance(FloatingButton::class),
            )
        }
    }

    @Suppress("CyclomaticComplexMethod", "LongMethod")
    private fun <T : Embedded.RowStyle> getEmbeddedAppearance(
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
        disclosureColor: Int? = null,
        checkmarkInsetDp: Float? = null,
        spacingDp: Float? = null
    ): Embedded {
        val row = when (type) {
            FlatWithRadio::class -> FlatWithRadio.Builder()
                .separatorThicknessDp(separatorThicknessDp ?: FlatWithRadio.default.separatorThicknessDp)
                .startSeparatorInsetDp(startSeparatorInset ?: FlatWithRadio.default.startSeparatorInsetDp)
                .endSeparatorInsetDp(endSeparatorInset ?: FlatWithRadio.default.endSeparatorInsetDp)
                .topSeparatorEnabled(topSeparatorEnabled ?: FlatWithRadio.default.topSeparatorEnabled)
                .bottomSeparatorEnabled(bottomSeparatorEnabled ?: FlatWithRadio.default.bottomSeparatorEnabled)
                .additionalVerticalInsetsDp(
                    additionalVerticalInsetsDp ?: FlatWithRadio.default.additionalVerticalInsetsDp
                )
                .horizontalInsetsDp(horizontalInsetsDp ?: FlatWithRadio.default.horizontalInsetsDp)
                .colorsLight(
                    FlatWithRadio.Colors(
                        separatorColor = separatorColor ?: FlatWithRadio.default.colorsLight.separatorColor,
                        selectedColor = selectedColor ?: FlatWithRadio.default.colorsLight.selectedColor,
                        unselectedColor = unselectedColor ?: FlatWithRadio.default.colorsLight.unselectedColor
                    )
                )
                .colorsDark(FlatWithRadio.default.colorsDark)
                .build()
            FlatWithCheckmark::class -> FlatWithCheckmark.Builder()
                .separatorThicknessDp(separatorThicknessDp ?: FlatWithCheckmark.default.separatorThicknessDp)
                .startSeparatorInsetDp(startSeparatorInset ?: FlatWithCheckmark.default.startSeparatorInsetDp)
                .endSeparatorInsetDp(endSeparatorInset ?: FlatWithCheckmark.default.endSeparatorInsetDp)
                .topSeparatorEnabled(topSeparatorEnabled ?: FlatWithCheckmark.default.topSeparatorEnabled)
                .bottomSeparatorEnabled(
                    bottomSeparatorEnabled ?: FlatWithCheckmark.default.bottomSeparatorEnabled
                )
                .checkmarkInsetDp(checkmarkInsetDp ?: FlatWithCheckmark.default.checkmarkInsetDp)
                .additionalVerticalInsetsDp(
                    additionalVerticalInsetsDp ?: FlatWithCheckmark.default.additionalVerticalInsetsDp
                )
                .horizontalInsetsDp(horizontalInsetsDp ?: FlatWithCheckmark.default.horizontalInsetsDp)
                .colorsLight(
                    FlatWithCheckmark.Colors(
                        separatorColor = separatorColor ?: FlatWithCheckmark.default.colorsLight.separatorColor,
                        checkmarkColor = checkmarkColor ?: FlatWithCheckmark.default.colorsLight.checkmarkColor
                    )
                )
                .colorsDark(FlatWithCheckmark.default.colorsDark)
                .build()
            FlatWithDisclosure::class -> FlatWithDisclosure.Builder()
                .separatorThicknessDp(separatorThicknessDp ?: FlatWithDisclosure.default.separatorThicknessDp)
                .startSeparatorInsetDp(startSeparatorInset ?: FlatWithDisclosure.default.startSeparatorInsetDp)
                .endSeparatorInsetDp(endSeparatorInset ?: FlatWithDisclosure.default.endSeparatorInsetDp)
                .topSeparatorEnabled(topSeparatorEnabled ?: FlatWithDisclosure.default.topSeparatorEnabled)
                .bottomSeparatorEnabled(
                    bottomSeparatorEnabled ?: FlatWithDisclosure.default.bottomSeparatorEnabled
                )
                .additionalVerticalInsetsDp(
                    additionalVerticalInsetsDp ?: FlatWithDisclosure.default.additionalVerticalInsetsDp
                )
                .horizontalInsetsDp(horizontalInsetsDp ?: FlatWithDisclosure.default.horizontalInsetsDp)
                .colorsLight(
                    FlatWithDisclosure.Colors(
                        separatorColor = separatorColor ?: FlatWithDisclosure.default.colorsLight.separatorColor,
                        disclosureColor = disclosureColor ?: FlatWithDisclosure.default.colorsLight.disclosureColor
                    )
                )
                .colorsDark(FlatWithDisclosure.default.colorsDark)
                .build()
            else -> FloatingButton.Builder()
                .spacingDp(spacingDp ?: FloatingButton.default.spacingDp)
                .additionalInsetsDp(additionalVerticalInsetsDp ?: FloatingButton.default.additionalInsetsDp)
                .build()
        }

        return Embedded(row)
    }
}
