package com.stripe.android.paymentsheet.verticalmode

import android.os.Build
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertAll
import androidx.compose.ui.test.isSelected
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onChildren
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import com.google.common.truth.Truth.assertThat
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadataFactory
import com.stripe.android.lpmfoundations.paymentmethod.definitions.CardDefinition
import com.stripe.android.model.PaymentIntentFixtures
import com.stripe.android.model.PaymentMethodFixtures
import com.stripe.android.paymentelement.ExperimentalEmbeddedPaymentElementApi
import com.stripe.android.paymentsheet.PaymentSheet.Appearance.Embedded
import com.stripe.android.paymentsheet.ViewActionRecorder
import com.stripe.android.paymentsheet.forms.FormFieldValues
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.ui.transformToPaymentSelection
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.Q])
class PaymentMethodEmbeddedLayoutUITest {
    @get:Rule
    val composeRule = createComposeRule()

    private val helper = PaymentMethodLayoutUITestHelper(
        composeRule,
        true
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
            tag = TEST_TAG_PAYMENT_METHOD_EMBEDDED_LAYOUT,
            childCount = 4
        )
    }

    @Test
    fun savedPaymentMethodIsSelected_whenSelectionIsSavedPm() {
        helper.savedPaymentMethodIsSelected_whenSelectionIsSavedPm()
    }

    @Test
    fun correctLPMIsSelected() {
        helper.correctLPMIsSelected(TEST_TAG_PAYMENT_METHOD_EMBEDDED_LAYOUT)
    }
}
