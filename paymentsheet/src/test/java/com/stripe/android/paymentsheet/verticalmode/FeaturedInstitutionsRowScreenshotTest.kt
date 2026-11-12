package com.stripe.android.paymentsheet.verticalmode

import android.graphics.Bitmap
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.stripe.android.core.strings.resolvableString
import com.stripe.android.paymentsheet.PaymentSheet.Appearance.Embedded
import com.stripe.android.paymentsheet.PaymentSheet.Appearance.Embedded.RowStyle
import com.stripe.android.paymentsheet.featuredinstitutions.FeaturedInstitutionIcon
import com.stripe.android.paymentsheet.featuredinstitutions.FeaturedInstitutions
import com.stripe.android.screenshottesting.PaparazziRule
import com.stripe.android.screenshottesting.SystemAppearance
import com.stripe.android.ui.core.R
import com.stripe.android.uicore.image.DefaultStripeImageLoader
import org.junit.Rule
import org.junit.Test

internal class FeaturedInstitutionsRowScreenshotTest {

    @get:Rule
    val paparazziRule = PaparazziRule(
        SystemAppearance.entries,
        boxModifier = Modifier
            .padding(16.dp)
            .fillMaxWidth()
    )

    @Test
    fun testFlatWithRadio() {
        snapshotBankRow(appearance = Embedded(RowStyle.FlatWithRadio.default))
    }

    @Test
    fun testFloatingButton() {
        snapshotBankRow(appearance = Embedded(RowStyle.FloatingButton.default))
    }

    @Test
    fun testFlatWithCheckmark() {
        snapshotBankRow(appearance = Embedded(RowStyle.FlatWithCheckmark.default))
    }

    @Test
    fun testFlatWithCheckmarkSelected() {
        snapshotBankRow(appearance = Embedded(RowStyle.FlatWithCheckmark.default), isSelected = true)
    }

    @Test
    fun testFlatWithDisclosure() {
        snapshotBankRow(appearance = Embedded(RowStyle.FlatWithDisclosure.default))
    }

    @Test
    fun testFlatWithRadioSelected() {
        snapshotBankRow(appearance = Embedded(RowStyle.FlatWithRadio.default), isSelected = true)
    }

    @Test
    fun testPaymentSheetRows() {
        paparazziRule.snapshot {
            NewPaymentMethodVerticalLayoutUI(
                paymentMethods = listOf(
                    bankDisplayablePaymentMethod(),
                    bankDisplayablePaymentMethod().copy(code = "link_selected"),
                ),
                selectedIndex = 1,
                isEnabled = true,
                imageLoader = DefaultStripeImageLoader(LocalContext.current),
            )
        }
    }

    @Test
    fun testSelectedWithChangeButton() {
        paparazziRule.snapshot {
            Column {
                EmbeddedNewPaymentMethodRowButtonsLayoutUi(
                    paymentMethods = listOf(bankDisplayablePaymentMethod()),
                    selection = PaymentMethodVerticalLayoutInteractor.Selection.New(
                        code = "link",
                        changeDetails = "Chase •••• 6789",
                        canBeChanged = true,
                    ),
                    isEnabled = true,
                    imageLoader = DefaultStripeImageLoader(LocalContext.current),
                    appearance = Embedded(RowStyle.FlatWithRadio.default),
                )
            }
        }
    }

    @Test
    fun testNarrowWidth() {
        paparazziRule.snapshot {
            Box(Modifier.width(200.dp)) {
                BankRow(appearance = Embedded(RowStyle.FlatWithRadio.default), isSelected = false)
            }
        }
    }

    @Test
    fun testIncentiveReplacesIcons() {
        paparazziRule.snapshot {
            BankRow(
                appearance = Embedded(RowStyle.FlatWithRadio.default),
                isSelected = false,
                promoBadge = "$5",
            )
        }
    }

    @Test
    fun testRtl() {
        paparazziRule.snapshot {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                BankRow(appearance = Embedded(RowStyle.FlatWithRadio.default), isSelected = false)
            }
        }
    }

    private fun snapshotBankRow(appearance: Embedded, isSelected: Boolean = false) {
        paparazziRule.snapshot {
            BankRow(appearance = appearance, isSelected = isSelected)
        }
    }

    @Composable
    private fun BankRow(
        appearance: Embedded,
        isSelected: Boolean,
        promoBadge: String? = null,
    ) {
        val displayablePaymentMethod = bankDisplayablePaymentMethod().copy(promoBadge = promoBadge)
        NewPaymentMethodRowButton(
            isEnabled = true,
            isSelected = isSelected,
            displayablePaymentMethod = displayablePaymentMethod.copy(
                // The incentive promo badge replaces the icons.
                featuredInstitutions = displayablePaymentMethod.featuredInstitutions.takeIf { promoBadge == null },
            ),
            imageLoader = DefaultStripeImageLoader(LocalContext.current),
            appearance = appearance,
        )
    }

    private fun bankDisplayablePaymentMethod(): DisplayablePaymentMethod {
        return DisplayablePaymentMethod(
            code = "link",
            displayName = R.string.stripe_paymentsheet_payment_method_instant_debits.resolvableString,
            iconResource = R.drawable.stripe_ic_paymentsheet_pm_bank,
            iconResourceNight = null,
            lightThemeIconUrl = null,
            darkThemeIconUrl = null,
            iconRequiresTinting = true,
            featuredInstitutions = featuredInstitutions(),
            isExpanded = false,
            onClick = {},
        )
    }

    private fun featuredInstitutions(): FeaturedInstitutions {
        return FeaturedInstitutions.preloaded(
            icons = ICON_COLORS.mapIndexed { index, color ->
                val bitmap = Bitmap.createBitmap(48, 48, Bitmap.Config.ARGB_8888).apply {
                    eraseColor(color)
                }
                FeaturedInstitutionIcon(id = "bank_$index", bitmap = bitmap.asImageBitmap())
            }
        )
    }

    private companion object {
        val ICON_COLORS = listOf(
            0xFF117ACA.toInt(),
            0xFFE31837.toInt(),
            0xFFFFCD41.toInt(),
            0xFF004977.toInt(),
            0xFF2E7D32.toInt(),
        )
    }
}
