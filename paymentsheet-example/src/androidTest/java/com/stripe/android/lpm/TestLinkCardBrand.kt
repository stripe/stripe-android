package com.stripe.android.lpm

import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.stripe.android.BasePlaygroundTest
import com.stripe.android.model.PaymentMethod
import com.stripe.android.paymentsheet.example.playground.settings.DEFAULT_BILLING_ADDRESS_PHONE
import com.stripe.android.paymentsheet.paymentdatacollection.ach.TEST_TAG_ACCOUNT_DETAILS
import com.stripe.android.test.core.AuthorizeAction
import com.stripe.android.test.core.DEFAULT_UI_TIMEOUT
import com.stripe.android.utils.ForceNativeBankFlowTestRule
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.UUID

@RunWith(AndroidJUnit4::class)
internal class TestLinkCardBrand : BasePlaygroundTest() {

    private val supportedPaymentMethods = listOf(PaymentMethod.Type.Card)

    @get:Rule
    val forceNativeBankFlowTestRule = ForceNativeBankFlowTestRule(
        context = ApplicationProvider.getApplicationContext()
    )

    @Test
    fun testLinkCardBrandSuccess() {
        val email = "email_${UUID.randomUUID()}@example.com"

        testDriver.confirmLinkBankPayment(
            testParameters = createLinkBankPaymentTestParameters(
                email = email,
                phone = null,
                supportedPaymentMethods = supportedPaymentMethods,
            ),
            afterAuthorization = { _, _ ->
                rules.compose.waitUntil(DEFAULT_UI_TIMEOUT.inWholeMilliseconds) {
                    rules.compose
                        .onAllNodesWithTag(TEST_TAG_ACCOUNT_DETAILS)
                        .fetchSemanticsNodes(atLeastOneRootRequired = false)
                        .isNotEmpty()
                }
            }
        )
    }

    @Test
    fun testLinkCardBrandCancelAllowsUserToContinue() {
        val email = "email_${UUID.randomUUID()}@email.com"

        testDriver.signUpForLink(
            createLinkBankSignUpTestParameters(
                email = email,
                supportedPaymentMethods = supportedPaymentMethods,
            )
        )

        testDriver.confirmLinkBankPayment(
            testParameters = createLinkBankPaymentTestParameters(
                email = email,
                phone = DEFAULT_BILLING_ADDRESS_PHONE,
                supportedPaymentMethods = supportedPaymentMethods,
            ).copy(authorizationAction = AuthorizeAction.Cancel),
            afterAuthorization = { selectors, _ ->
                selectors.buyButton.waitProcessingComplete()
            }
        )
    }
}
