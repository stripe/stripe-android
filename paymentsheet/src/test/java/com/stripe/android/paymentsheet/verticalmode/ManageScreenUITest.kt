package com.stripe.android.paymentsheet.verticalmode

import android.os.Build
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onChildren
import androidx.compose.ui.test.onNodeWithTag
import com.google.common.truth.Truth.assertThat
import com.stripe.android.model.PaymentMethodFixtures
import com.stripe.android.paymentsheet.DisplayableSavedPaymentMethod
import com.stripe.android.paymentsheet.FakeManageScreenInteractor
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.Test

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.Q])
class ManageScreenUITest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun allSavedPaymentMethodsAreShown() {
        val displayableSavedPaymentMethods = PaymentMethodFixtures.createCards(4).map {
            DisplayableSavedPaymentMethod(
                displayName = it.card!!.last4!!,
                paymentMethod = it,
                isCbcEligible = false
            )
        }
        val manageScreenInteractor = FakeManageScreenInteractor(
            paymentMethods = displayableSavedPaymentMethods
        )

        composeRule.setContent {
            ManageScreenUI(interactor = manageScreenInteractor)
        }

        assertThat(
            composeRule.onNodeWithTag(TEST_TAG_MANAGE_SCREEN_SAVED_PMS_LIST).onChildren().fetchSemanticsNodes().size
        ).isEqualTo(displayableSavedPaymentMethods.size)

        for (savedPaymentMethod in displayableSavedPaymentMethods) {
            composeRule.onNodeWithTag(
                "${TEST_TAG_SAVED_PAYMENT_METHOD_ROW_BUTTON}_${savedPaymentMethod.paymentMethod.id}"
            ).assertExists()
        }
    }
}
