package com.stripe.android.paymentsheet.utils

import android.content.Context
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
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

    abstract fun assertSetDefaultPaymentMethodCheckboxDisplayedAndDisabled(
        composeTestRule: ComposeTestRule,
        paymentMethod: PaymentMethod,
    )

    abstract fun assertDefaultPaymentMethodBadgeDisplayed(
        composeTestRule: ComposeTestRule,
    )

    abstract fun payWithNewCardWithSavedPaymentMethods(
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

        override fun assertSetDefaultPaymentMethodCheckboxDisplayedAndDisabled(
            composeTestRule: ComposeTestRule,
            paymentMethod: PaymentMethod,
        ) {
            val savedPaymentMethodsPage = SavedPaymentMethodsPage(composeTestRule)
            val editPage = EditPage(composeTestRule)

            navigateToEditPage(
                savedPaymentMethodsPage = savedPaymentMethodsPage,
                editPage = editPage,
                paymentMethod = paymentMethod,
            )

            assertSetAsDefaultCheckboxDisplayedAndDisabled(editPage = editPage)
        }

        override fun setDefaultPaymentMethod(
            composeTestRule: ComposeTestRule,
            newDefaultPaymentMethod: PaymentMethod
        ) {
            val savedPaymentMethodsPage = SavedPaymentMethodsPage(composeTestRule)
            val editPage = EditPage(composeTestRule)

            navigateToEditPage(
                savedPaymentMethodsPage = savedPaymentMethodsPage,
                editPage = editPage,
                paymentMethod = newDefaultPaymentMethod,
            )

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

        private fun navigateToEditPage(
            savedPaymentMethodsPage: SavedPaymentMethodsPage,
            editPage: EditPage,
            paymentMethod: PaymentMethod,
        ) {
            savedPaymentMethodsPage.onEditButton().performClick()

            savedPaymentMethodsPage.onModifyBadgeFor(
                paymentMethod.card!!.last4!!
            ).performClick()

            editPage.waitUntilVisible()
        }

        override fun payWithNewCardWithSavedPaymentMethods(
            composeTestRule: ComposeTestRule,
        ) {
            val savedPaymentMethodsPage = SavedPaymentMethodsPage(composeTestRule)

            savedPaymentMethodsPage.clickNewCardButton()
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

        override fun assertSetDefaultPaymentMethodCheckboxDisplayedAndDisabled(
            composeTestRule: ComposeTestRule,
            paymentMethod: PaymentMethod
        ) {
            val verticalModePage = VerticalModePage(composeTestRule)
            val managePage = ManagePage(composeTestRule)
            val editPage = EditPage(composeTestRule)

            navigateToEditPage(
                verticalModePage = verticalModePage,
                managePage = managePage,
                editPage = editPage,
                paymentMethod = paymentMethod,
            )

            assertSetAsDefaultCheckboxDisplayedAndDisabled(editPage = editPage)
        }

        override fun setDefaultPaymentMethod(composeTestRule: ComposeTestRule, newDefaultPaymentMethod: PaymentMethod) {
            val verticalModePage = VerticalModePage(composeTestRule)
            val managePage = ManagePage(composeTestRule)
            val editPage = EditPage(composeTestRule)

            navigateToEditPage(
                verticalModePage = verticalModePage,
                managePage = managePage,
                editPage = editPage,
                paymentMethod = newDefaultPaymentMethod,
            )

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

        private fun navigateToEditPage(
            verticalModePage: VerticalModePage,
            managePage: ManagePage,
            editPage: EditPage,
            paymentMethod: PaymentMethod,
        ) {
            verticalModePage.clickViewMore()
            managePage.waitUntilVisible()

            managePage.clickEdit()
            managePage.clickEdit(paymentMethod.id!!)

            editPage.waitUntilVisible()
        }

        override fun payWithNewCardWithSavedPaymentMethods(
            composeTestRule: ComposeTestRule,
        ) {
            val verticalModePage = VerticalModePage(composeTestRule)

            verticalModePage.clickNewPaymentMethodButton(PaymentMethod.Type.Card.code)
        }
    }

    private companion object {
        fun assertExactlyOneDefaultLabelShown(composeTestRule: ComposeTestRule) {
            composeTestRule.onAllNodesWithTag(
                TEST_TAG_DEFAULT_PAYMENT_METHOD_LABEL,
                useUnmergedTree = true
            ).assertCountEquals(1)
        }

        fun assertSetAsDefaultCheckboxDisplayedAndDisabled(
            editPage: EditPage,
        ) {
            editPage.onSetAsDefaultCheckbox().assertIsDisplayed()
            editPage.onSetAsDefaultCheckbox().assertIsNotEnabled()
        }
    }
}

internal object PaymentSheetLayoutTypeProvider : TestParameterValuesProvider() {
    override fun provideValues(context: Context?): List<PaymentSheetLayoutType> {
       return listOf(PaymentSheetLayoutType.Vertical(), PaymentSheetLayoutType.Horizontal())
    }
}
