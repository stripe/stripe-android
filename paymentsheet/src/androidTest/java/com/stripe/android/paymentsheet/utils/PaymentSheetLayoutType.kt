package com.stripe.android.paymentsheet.utils

import android.content.Context
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.isEnabled
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.espresso.Espresso
import com.google.testing.junit.testparameterinjector.TestParameterValuesProvider
import com.stripe.android.model.PaymentMethod
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.ui.SAVED_PAYMENT_METHOD_CARD_TEST_TAG
import com.stripe.android.paymentsheet.ui.TEST_TAG_DEFAULT_PAYMENT_METHOD_LABEL
import com.stripe.android.paymentsheet.ui.getLabel
import com.stripe.paymentelementtestpages.EditPage
import com.stripe.paymentelementtestpages.ManagePage
import com.stripe.paymentelementtestpages.SavedPaymentMethodsPage
import com.stripe.paymentelementtestpages.VerticalModePage

internal sealed class PaymentSheetLayoutType(val paymentMethodLayout: PaymentSheet.PaymentMethodLayout) {

    abstract fun assertHasSelectedPaymentMethod(
        composeTestRule: ComposeTestRule,
        context: Context,
        paymentMethod: PaymentMethod
    )

    abstract fun setDefaultPaymentMethod(
        composeTestRule: ComposeTestRule,
        newDefaultPaymentMethod: PaymentMethod,
    )

    abstract fun assertDefaultPaymentMethodBadgeDisplayed(
        composeTestRule: ComposeTestRule,
    )

    class Horizontal : PaymentSheetLayoutType(paymentMethodLayout = PaymentSheet.PaymentMethodLayout.Horizontal) {
        override fun assertHasSelectedPaymentMethod(
            composeTestRule: ComposeTestRule,
            context: Context,
            paymentMethod: PaymentMethod,
        ) {
            val label = paymentMethod.getLabel()?.resolve(context)
            composeTestRule
                .onNodeWithTag("${SAVED_PAYMENT_METHOD_CARD_TEST_TAG}_$label")
                .assertIsSelected()
        }

        override fun setDefaultPaymentMethod(
            composeTestRule: ComposeTestRule,
            newDefaultPaymentMethod: PaymentMethod
        ) {
            val savedPaymentMethodsPage = SavedPaymentMethodsPage(composeTestRule)
            val editPage = EditPage(composeTestRule)

            savedPaymentMethodsPage.onEditButton().performClick()

            savedPaymentMethodsPage.onModifyBadgeFor(
                newDefaultPaymentMethod.card!!.last4!!
            ).performClick()

            editPage.waitUntilVisible()
            editPage.clickSetAsDefaultCheckbox()

            editPage.update()

            // Reset screen to non-editing state.
            savedPaymentMethodsPage.waitUntilVisible()
            savedPaymentMethodsPage.onEditButton().performClick()
        }

        override fun assertDefaultPaymentMethodBadgeDisplayed(composeTestRule: ComposeTestRule) {
            val savedPaymentMethodsPage = SavedPaymentMethodsPage(composeTestRule)

            savedPaymentMethodsPage.onEditButton().performClick()

            assertExactlyOneDefaultLabelShown(composeTestRule)
        }
    }

    class Vertical : PaymentSheetLayoutType(paymentMethodLayout = PaymentSheet.PaymentMethodLayout.Vertical) {
        override fun assertHasSelectedPaymentMethod(
            composeTestRule: ComposeTestRule,
            context: Context,
            paymentMethod: PaymentMethod
        ) {
            val verticalModePage = VerticalModePage(composeTestRule)
            val managePage = ManagePage(composeTestRule)

            verticalModePage.assertHasSavedPaymentMethods()
            verticalModePage.assertHasSelectedSavedPaymentMethod(paymentMethod.id!!)
            verticalModePage.assertPrimaryButton(isEnabled())

            verticalModePage.clickViewMore()
            managePage.waitUntilVisible()
            verticalModePage.assertHasSelectedSavedPaymentMethod(paymentMethod.id!!)

            Espresso.pressBack()
        }

        override fun setDefaultPaymentMethod(composeTestRule: ComposeTestRule, newDefaultPaymentMethod: PaymentMethod) {
            val verticalModePage = VerticalModePage(composeTestRule)
            val managePage = ManagePage(composeTestRule)
            val editPage = EditPage(composeTestRule)

            verticalModePage.clickViewMore()
            managePage.waitUntilVisible()

            managePage.clickEdit()
            managePage.clickEdit(newDefaultPaymentMethod.id!!)

            editPage.waitUntilVisible()
            editPage.clickSetAsDefaultCheckbox()

            editPage.update()

            // Go back to initial screen.
            managePage.waitUntilVisible()
            managePage.clickDone()
            Espresso.pressBack()
        }

        override fun assertDefaultPaymentMethodBadgeDisplayed(composeTestRule: ComposeTestRule) {
            val verticalModePage = VerticalModePage(composeTestRule)
            val managePage = ManagePage(composeTestRule)

            verticalModePage.waitUntilVisible()
            verticalModePage.clickViewMore()
            managePage.waitUntilVisible()

            assertExactlyOneDefaultLabelShown(composeTestRule)
        }
    }

    private companion object {
        fun assertExactlyOneDefaultLabelShown(composeTestRule: ComposeTestRule) {
            composeTestRule.onAllNodesWithTag(
                TEST_TAG_DEFAULT_PAYMENT_METHOD_LABEL,
                useUnmergedTree = true
            ).assertCountEquals(1)
        }
    }
}

internal object PaymentSheetLayoutTypeProvider : TestParameterValuesProvider() {
    override fun provideValues(context: Context?): List<PaymentSheetLayoutType> {
       return listOf(PaymentSheetLayoutType.Vertical(), PaymentSheetLayoutType.Horizontal())
    }
}
