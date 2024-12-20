package com.stripe.android.paymentsheet.verticalmode

import android.os.Build
import androidx.compose.ui.test.junit4.createComposeRule
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.Q])
internal class PaymentMethodVerticalLayoutUITest {

    @get:Rule
    val composeRule = createComposeRule()

    private val helper = PaymentMethodLayoutUITestHelper(
        composeRule,
        false
    )

    @Test
    fun clickingOnViewMore_transitionsToManageScreen() {
        helper.clickingOnViewMore_transitionsToManageScreen()
    }

    @Test
    fun oneSavedPm_canBeRemoved_buttonIsEdit_callsOnManageOneSavedPm() {
        helper.oneSavedPm_canBeRemoved_buttonIsEdit_callsOnManageOneSavedPm()
    }

    @Test
    fun oneSavedPm_cannotBeEdited_noSavedPaymentMethodButton() {
        helper.oneSavedPm_cannotBeEdited_noSavedPaymentMethodButton()
    }

    @Test
    fun clickingOnNewPaymentMethod_callsOnClick() {
        helper.clickingOnNewPaymentMethod_callsOnClick()
    }

    @Test
    fun clickingSavedPaymentMethod_callsSelectSavedPaymentMethod() {
        helper.clickingSavedPaymentMethod_callsSelectSavedPaymentMethod()
    }

    @Test
    fun allPaymentMethodsAreShown() {
        helper.allPaymentMethodsAreShown(
            tag = TEST_TAG_NEW_PAYMENT_METHOD_VERTICAL_LAYOUT_UI,
            childCount = 3
        )
    }

    @Test
    fun savedPaymentMethodIsSelected_whenSelectionIsSavedPm() {
        helper.savedPaymentMethodIsSelected_whenSelectionIsSavedPm()
    }

    @Test
    fun correctLPMIsSelected() {
        helper.correctLPMIsSelected(TEST_TAG_NEW_PAYMENT_METHOD_VERTICAL_LAYOUT_UI)
    }
}
