package com.stripe.android.paymentsheet.verticalmode

import com.stripe.android.model.PaymentMethodFixtures
import com.stripe.android.paymentsheet.DisplayableSavedPaymentMethod
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.screenshottesting.PaparazziRule
import com.stripe.android.utils.MockPaymentMethodsFactory
import com.stripe.android.utils.screenshots.PaymentSheetAppearance
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.mock

internal class PaymentMethodVerticalLayoutUIScreenshotTest {
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
    fun testSavedAndNewPms() {
        paparazziRule.snapshot {
            PaymentMethodVerticalLayoutUI(
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
            )
        }
    }

    @Test
    fun testDefault() {
        paparazziRule.snapshot {
            PaymentMethodVerticalLayoutUI(
                paymentMethods = paymentMethods,
                displayedSavedPaymentMethod = PaymentMethodFixtures.defaultDisplayableCard(),
                savedPaymentMethodAction =
                PaymentMethodVerticalLayoutInteractor.SavedPaymentMethodAction.MANAGE_ALL,
                selection = PaymentSelection.Saved(savedPaymentMethod.paymentMethod),
                isEnabled = true,
                onViewMorePaymentMethods = {},
                onSelectSavedPaymentMethod = {},
                onManageOneSavedPaymentMethod = {},
                imageLoader = mock(),
            )
        }
    }

    @Test
    fun testNewPmsWithPromoBadge() {
        val paymentMethodsWithBadge = paymentMethods.map { pm ->
            pm.copy(promoBadge = "$5".takeIf { pm.code == "affirm" })
        }

        paparazziRule.snapshot {
            PaymentMethodVerticalLayoutUI(
                paymentMethods = paymentMethodsWithBadge,
                displayedSavedPaymentMethod = savedPaymentMethod,
                savedPaymentMethodAction =
                PaymentMethodVerticalLayoutInteractor.SavedPaymentMethodAction.MANAGE_ALL,
                selection = PaymentSelection.Saved(savedPaymentMethod.paymentMethod),
                isEnabled = true,
                onViewMorePaymentMethods = {},
                onSelectSavedPaymentMethod = {},
                onManageOneSavedPaymentMethod = {},
                imageLoader = mock(),
            )
        }
    }
}
