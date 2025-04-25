package com.stripe.android.paymentsheet.ui

import android.os.Build
import androidx.compose.ui.test.assertAny
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onChildren
import androidx.compose.ui.test.onNodeWithTag
import com.stripe.android.model.PaymentMethodFixtures
import com.stripe.android.model.PaymentMethodFixtures.toDisplayableSavedPaymentMethod
import com.stripe.android.testing.createComposeCleanupRule
import com.stripe.android.ui.core.elements.TEST_TAG_SIMPLE_DIALOG
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.Test

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.Q])
class RemovePaymentMethodDialogUITest {

    @get:Rule
    val composeRule = createComposeRule()

    @get:Rule
    val composeCleanupRule = createComposeCleanupRule()

    @Test
    fun removeDescription_usesSelectedBrandIfAvailable() {
        val paymentMethod = PaymentMethodFixtures
            .CARD_WITH_NETWORKS_PAYMENT_METHOD
            .toDisplayableSavedPaymentMethod()

        composeRule.setContent {
            RemovePaymentMethodDialogUI(
                paymentMethod = paymentMethod,
                onConfirmListener = {},
                onDismissListener = {}
            )
        }

        composeRule.onNodeWithTag(TEST_TAG_SIMPLE_DIALOG).onChildren().assertAny(
            hasText("Cartes Bancaires ····4242")
        )
    }
}
