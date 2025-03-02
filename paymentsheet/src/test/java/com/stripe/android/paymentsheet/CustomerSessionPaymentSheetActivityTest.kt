package com.stripe.android.paymentsheet

import android.app.Application
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasAnyDescendant
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import com.stripe.android.model.CardBrand
import com.stripe.android.model.PaymentMethod
import com.stripe.android.networktesting.NetworkRule
import com.stripe.android.networktesting.RequestMatchers
import com.stripe.android.paymentsheet.state.PaymentElementLoader
import com.stripe.android.paymentsheet.ui.PAYMENT_SHEET_EDIT_BUTTON_TEST_TAG
import com.stripe.android.paymentsheet.ui.SAVED_PAYMENT_OPTION_TAB_LAYOUT_TEST_TAG
import com.stripe.android.paymentsheet.ui.SAVED_PAYMENT_OPTION_TEST_TAG
import com.stripe.android.paymentsheet.ui.TEST_TAG_MODIFY_BADGE
import com.stripe.android.paymentsheet.ui.UPDATE_PM_REMOVE_BUTTON_TEST_TAG
import com.stripe.android.testing.PaymentConfigurationTestRule
import com.stripe.android.testing.PaymentMethodFactory
import com.stripe.paymentelementtestpages.EditPage
import com.stripe.paymentelementtestpages.ManagePage
import com.stripe.paymentelementtestpages.VerticalModePage
import org.json.JSONArray
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

@OptIn(ExperimentalCustomerSessionApi::class)
@RunWith(RobolectricTestRunner::class)
internal class CustomerSessionPaymentSheetActivityTest {
    private val applicationContext = ApplicationProvider.getApplicationContext<Application>()

    private val composeTestRule = createAndroidComposeRule<PaymentSheetActivity>()
    private val networkRule = NetworkRule()

    @get:Rule
    val ruleChain: RuleChain = RuleChain
        .outerRule(composeTestRule)
        .around(networkRule)
        .around(PaymentConfigurationTestRule(applicationContext))

    @Test
    fun `When multiple PMs with remove permissions and can remove last PM, should all be enabled when editing`() =
        runTest(
            cards = listOf(
                PaymentMethodFactory.card(last4 = "4242"),
                PaymentMethodFactory.card(last4 = "5544", addCbcNetworks = true),
            ),
            isPaymentMethodRemoveEnabled = true,
            canRemoveLastPaymentMethodConfig = true,
            canRemoveLastPaymentMethodServer = true,
        ) {
            composeTestRule.onEditButton().performClick()

            composeTestRule.onSavedPaymentMethod(last4 = "4242").assertIsEnabled()
            composeTestRule.onSavedPaymentMethod(last4 = "5544").assertIsEnabled()
        }

    @Test
    fun `When single PM with remove permissions and can remove last from sources, should be enabled when editing`() =
        runTest(
            cards = listOf(
                PaymentMethodFactory.card(last4 = "4242"),
            ),
            isPaymentMethodRemoveEnabled = true,
            canRemoveLastPaymentMethodConfig = true,
            canRemoveLastPaymentMethodServer = true,
        ) {
            composeTestRule.onEditButton().performClick()

            composeTestRule.onSavedPaymentMethod(last4 = "4242").assertIsEnabled()
        }

    @Test
    fun `When single PM with remove permissions and cannot remove last PM from server, edit should not be shown`() =
        runTest(
            cards = listOf(
                PaymentMethodFactory.card(last4 = "4242"),
            ),
            isPaymentMethodRemoveEnabled = true,
            canRemoveLastPaymentMethodConfig = true,
            canRemoveLastPaymentMethodServer = false,
        ) {
            composeTestRule.onEditButton().assertDoesNotExist()
        }

    @Test
    fun `When single PM with remove permissions & cannot remove last PM from config, edit should not be shown`() =
        runTest(
            cards = listOf(
                PaymentMethodFactory.card(last4 = "4242"),
            ),
            isPaymentMethodRemoveEnabled = true,
            canRemoveLastPaymentMethodConfig = false,
            canRemoveLastPaymentMethodServer = true,
        ) {
            composeTestRule.onEditButton().assertDoesNotExist()
        }

    @Test
    fun `When single PM with remove permissions but cannot remove last PM, edit button should not be displayed`() =
        runTest(
            cards = listOf(
                PaymentMethodFactory.card(last4 = "4242"),
            ),
            isPaymentMethodRemoveEnabled = true,
            canRemoveLastPaymentMethodConfig = false,
            canRemoveLastPaymentMethodServer = false,
        ) {
            composeTestRule.onEditButton().assertDoesNotExist()
        }

    @Test
    fun `When multiple PMs but no remove permissions, edit button should not be displayed`() =
        runTest(
            cards = listOf(
                PaymentMethodFactory.card(last4 = "4242"),
                PaymentMethodFactory.card(last4 = "5544"),
            ),
            isPaymentMethodRemoveEnabled = false,
            canRemoveLastPaymentMethodConfig = true,
            canRemoveLastPaymentMethodServer = false,
        ) {
            composeTestRule.onEditButton().assertDoesNotExist()
        }

    @Test
    fun `When multiple PMs with CBC card but no remove permissions, should allow editing all PMs`() =
        runTest(
            cards = listOf(
                PaymentMethodFactory.card(last4 = "4242", addCbcNetworks = true),
                PaymentMethodFactory.card(last4 = "5544"),
            ),
            isPaymentMethodRemoveEnabled = false,
            canRemoveLastPaymentMethodConfig = true,
            canRemoveLastPaymentMethodServer = false,
        ) {
            composeTestRule.onEditButton().performClick()

            val nonCbcCard = composeTestRule.onSavedPaymentMethod(last4 = "5544")
            nonCbcCard.assertIsEnabled()
            nonCbcCard.assertHasModifyBadge()

            val cbcCard = composeTestRule.onSavedPaymentMethod(last4 = "4242")

            cbcCard.assertIsEnabled()
            cbcCard.assertHasModifyBadge()
        }

    @Test
    fun `When multiple PMs with CBC card and has remove permissions, should be able to remove and edit CBC card`() =
        runTest(
            cards = listOf(
                PaymentMethodFactory.card(last4 = "4242", addCbcNetworks = true),
                PaymentMethodFactory.card(last4 = "5544"),
            ),
            isPaymentMethodRemoveEnabled = true,
            canRemoveLastPaymentMethodConfig = true,
            canRemoveLastPaymentMethodServer = false,
        ) {
            composeTestRule.onEditButton().performClick()

            composeTestRule.onSavedPaymentMethod(last4 = "5544").assertIsEnabled()

            val cbcCard = composeTestRule.onSavedPaymentMethod(last4 = "4242")

            cbcCard.assertIsEnabled()
            cbcCard.assertHasModifyBadge()

            composeTestRule.onModifyBadgeFor(last4 = "4242").performClick()

            composeTestRule.onUpdateScreenRemoveButton().assertIsEnabled()
        }

    @Test
    fun `When single CBC card, has remove permissions, and cannot remove last PM from server, can only edit`() =
        runTest(
            cards = listOf(
                PaymentMethodFactory.card(last4 = "4242", addCbcNetworks = true),
            ),
            isPaymentMethodRemoveEnabled = true,
            canRemoveLastPaymentMethodConfig = true,
            canRemoveLastPaymentMethodServer = false,
        ) {
            composeTestRule.onEditButton().performClick()

            val cbcCard = composeTestRule.onSavedPaymentMethod(last4 = "4242")

            cbcCard.assertIsEnabled()
            cbcCard.assertHasModifyBadge()

            composeTestRule.onModifyBadgeFor(last4 = "4242").performClick()

            composeTestRule.onUpdateScreenRemoveButton().assertDoesNotExist()
        }

    @Test
    fun `When single CBC card, has remove permissions, and cannot remove last PM from config, can only edit`() =
        runTest(
            cards = listOf(
                PaymentMethodFactory.card(last4 = "4242", addCbcNetworks = true),
            ),
            isPaymentMethodRemoveEnabled = true,
            canRemoveLastPaymentMethodConfig = false,
            canRemoveLastPaymentMethodServer = true,
        ) {
            composeTestRule.onEditButton().performClick()

            val cbcCard = composeTestRule.onSavedPaymentMethod(last4 = "4242")

            cbcCard.assertIsEnabled()
            cbcCard.assertHasModifyBadge()

            composeTestRule.onModifyBadgeFor(last4 = "4242").performClick()

            composeTestRule.onUpdateScreenRemoveButton().assertDoesNotExist()
        }

    @Test
    fun `When single CBC card, has remove permissions, and can remove last PM from all sources, can remove and edit`() =
        runTest(
            cards = listOf(
                PaymentMethodFactory.card(last4 = "4242", addCbcNetworks = true),
            ),
            isPaymentMethodRemoveEnabled = true,
            canRemoveLastPaymentMethodConfig = true,
            canRemoveLastPaymentMethodServer = true,
        ) {
            composeTestRule.onEditButton().performClick()

            val cbcCard = composeTestRule.onSavedPaymentMethod(last4 = "4242")

            cbcCard.assertIsEnabled()
            cbcCard.assertHasModifyBadge()

            composeTestRule.onModifyBadgeFor(last4 = "4242").performClick()

            composeTestRule.onUpdateScreenRemoveButton().assertIsEnabled()
        }

    @Test
    fun `When single CBC card but no remove permissions, can edit but not remove CBC card`() =
        runTest(
            cards = listOf(
                PaymentMethodFactory.card(last4 = "4242", addCbcNetworks = true),
            ),
            isPaymentMethodRemoveEnabled = false,
            canRemoveLastPaymentMethodConfig = true,
            canRemoveLastPaymentMethodServer = false,
        ) {
            composeTestRule.onEditButton().performClick()

            val cbcCard = composeTestRule.onSavedPaymentMethod(last4 = "4242")

            cbcCard.assertIsEnabled()
            cbcCard.assertHasModifyBadge()

            composeTestRule.onModifyBadgeFor(last4 = "4242").performClick()

            composeTestRule.onUpdateScreenRemoveButton().assertDoesNotExist()
        }

    @Test
    fun `When single CBC card has remove permissions but cannot remove last, can edit but not remove CBC card`() =
        runTest(
            cards = listOf(
                PaymentMethodFactory.card(last4 = "4242", addCbcNetworks = true),
            ),
            isPaymentMethodRemoveEnabled = true,
            canRemoveLastPaymentMethodConfig = false,
            canRemoveLastPaymentMethodServer = false,
        ) {
            composeTestRule.onEditButton().performClick()

            val cbcCard = composeTestRule.onSavedPaymentMethod(last4 = "4242")

            cbcCard.assertIsEnabled()
            cbcCard.assertHasModifyBadge()

            composeTestRule.onModifyBadgeFor(last4 = "4242").performClick()

            composeTestRule.onUpdateScreenRemoveButton().assertDoesNotExist()
        }

    private fun runTest(
        cards: List<PaymentMethod>,
        isPaymentMethodRemoveEnabled: Boolean = true,
        canRemoveLastPaymentMethodConfig: Boolean = true,
        canRemoveLastPaymentMethodServer: Boolean = true,
        defaultPaymentMethod: String? = null,
        test: (PaymentSheetActivity) -> Unit,
    ) {
        networkRule.enqueue(
            RequestMatchers.host("api.stripe.com"),
            RequestMatchers.method("GET"),
            RequestMatchers.path("/v1/elements/sessions"),
        ) { response ->
            response.setBody(
                createElementsSessionResponse(
                    cards = cards,
                    isPaymentMethodRemoveEnabled = isPaymentMethodRemoveEnabled,
                    canRemoveLastPaymentMethod = canRemoveLastPaymentMethodServer,
                    defaultPaymentMethod = defaultPaymentMethod,
                )
            )
        }

        val countDownLatch = CountDownLatch(1)

        ActivityScenario.launch<PaymentSheetActivity>(
            PaymentSheetContractV2().createIntent(
                ApplicationProvider.getApplicationContext(),
                PaymentSheetContractV2.Args(
                    initializationMode = PaymentElementLoader.InitializationMode.PaymentIntent(
                        clientSecret = "pi_1234_secret_5678",
                    ),
                    config = PaymentSheet.Configuration(
                        merchantDisplayName = "Merchant, Inc.",
                        customer = PaymentSheet.CustomerConfiguration.createWithCustomerSession(
                            id = "cus_1",
                            clientSecret = "cuss_1",
                        ),
                        allowsRemovalOfLastSavedPaymentMethod = canRemoveLastPaymentMethodConfig,
                        preferredNetworks = listOf(CardBrand.CartesBancaires, CardBrand.Visa),
                    ),
                    statusBarColor = PaymentSheetFixtures.STATUS_BAR_COLOR,
                )
            )
        ).use { scenario ->
            scenario.onActivity { activity ->
                composeTestRule.waitUntil(timeoutMillis = 2_000) {
                    composeTestRule
                        .onAllNodes(hasTestTag(SAVED_PAYMENT_OPTION_TAB_LAYOUT_TEST_TAG))
                        .fetchSemanticsNodes()
                        .isNotEmpty()
                }

                test(activity)

                countDownLatch.countDown()
            }

            countDownLatch.await(5, TimeUnit.SECONDS)
            networkRule.validate()
        }
    }

    private fun ComposeTestRule.onEditButton(): SemanticsNodeInteraction {
        return onNode(
            hasTestTag(PAYMENT_SHEET_EDIT_BUTTON_TEST_TAG)
        )
    }

    private fun ComposeTestRule.onUpdateScreenRemoveButton(): SemanticsNodeInteraction {
        return onNode(
            hasTestTag(UPDATE_PM_REMOVE_BUTTON_TEST_TAG)
        )
    }

    private fun SemanticsNodeInteraction.assertHasModifyBadge() {
        assert(hasAnyDescendant(hasTestTag(TEST_TAG_MODIFY_BADGE)))
    }

    private fun ComposeTestRule.onModifyBadgeFor(last4: String): SemanticsNodeInteraction {
        return onNode(
            hasTestTag(TEST_TAG_MODIFY_BADGE).and(hasAnyAncestor(savedPaymentMethodMatcher(last4)))
        )
    }

    private fun ComposeTestRule.onSavedPaymentMethod(last4: String): SemanticsNodeInteraction {
        return onNode(savedPaymentMethodMatcher(last4))
    }

    private fun savedPaymentMethodMatcher(last4: String): SemanticsMatcher {
        return hasTestTag(SAVED_PAYMENT_OPTION_TEST_TAG).and(hasText(last4, substring = true))
    }

    private companion object {
        @Suppress("LongMethod")
        fun createElementsSessionResponse(
            cards: List<PaymentMethod>,
            isPaymentMethodRemoveEnabled: Boolean,
            canRemoveLastPaymentMethod: Boolean,
            defaultPaymentMethod: String?,
        ): String {
            val cardsArray = JSONArray()

            cards.forEach { card ->
                cardsArray.put(PaymentMethodFactory.convertCardToJson(card))
            }

            val cardsStringified = cardsArray.toString(2)

            val isPaymentMethodRemoveStringified = isPaymentMethodRemoveEnabled.toFeatureState()
            val canRemoveLastPaymentMethodStringified = canRemoveLastPaymentMethod.toFeatureState()

            return """
                {
                  "business_name": "Mobile Example Account",
                  "google_pay_preference": "enabled",
                  "merchant_country": "US",
                  "merchant_currency": "usd",
                  "merchant_id": "acct_1HvTI7Lu5o3P18Zp",
                  "meta_pay_signed_container_context": null,
                  "order": null,
                  "ordered_payment_method_types_and_wallets": [
                    "card"
                  ],
                  "card_brand_choice": {
                    "eligible": true,
                    "preferred_networks": ["cartes_bancaires"]
                  },
                  "customer": {
                    "payment_methods": $cardsStringified,
                    "customer_session": {
                      "id": "cuss_654321",
                      "livemode": false,
                      "api_key": "ek_12345",
                      "api_key_expiry": 1899787184,
                      "customer": "cus_12345",
                      "components": {
                        "mobile_payment_element": {
                          "enabled": true,
                          "features": {
                            "payment_method_save": "enabled",
                            "payment_method_remove": "$isPaymentMethodRemoveStringified",
                            "payment_method_remove_last": "$canRemoveLastPaymentMethodStringified",
                            "payment_method_save_allow_redisplay_override": null,
                          }
                        },
                        "customer_sheet": {
                          "enabled": false,
                          "features": null
                        }
                      }
                    },
                    "default_payment_method": $defaultPaymentMethod
                  },
                  "payment_method_preference": {
                    "object": "payment_method_preference",
                    "country_code": "US",
                    "ordered_payment_method_types": [
                      "card"
                    ],
                    "payment_intent": {
                      "id": "pi_example",
                      "object": "payment_intent",
                      "amount": 5099,
                      "amount_details": {
                        "tip": {}
                      },
                      "automatic_payment_methods": {
                        "enabled": true
                      },
                      "canceled_at": null,
                      "cancellation_reason": null,
                      "capture_method": "automatic",
                      "client_secret": "pi_example_secret_example",
                      "confirmation_method": "automatic",
                      "created": 1674750417,
                      "currency": "usd",
                      "description": null,
                      "last_payment_error": null,
                      "livemode": false,
                      "next_action": null,
                      "payment_method": null,
                      "payment_method_options": {
                        "us_bank_account": {
                          "verification_method": "automatic"
                        }
                      },
                      "payment_method_types": [
                        "card"
                      ],
                      "processing": null,
                      "receipt_email": null,
                      "setup_future_usage": null,
                      "shipping": null,
                      "source": null,
                      "status": "requires_payment_method"
                    },
                    "type": "payment_intent"
                  },
                  "payment_method_specs": [
                    {
                      "async": false,
                      "fields": [],
                      "type": "card"
                    }
                  ],
                  "paypal_express_config": {
                    "client_id": null,
                    "paypal_merchant_id": null
                  },
                  "shipping_address_settings": {
                    "autocomplete_allowed": true
                  },
                  "unactivated_payment_method_types": []
                }
            """.trimIndent()
        }

        private fun Boolean.toFeatureState(): String {
            return if (this) {
                "enabled"
            } else {
                "disabled"
            }
        }
    }
}
