package com.stripe.android.paymentelement.embedded.sheet

import android.app.Activity
import android.app.Application
import android.os.Bundle
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso
import androidx.test.espresso.Espresso.onIdle
import com.google.common.truth.Truth.assertThat
import com.stripe.android.checkouttesting.checkoutUpdate
import com.stripe.android.core.strings.resolvableString
import com.stripe.android.isInstanceOf
import com.stripe.android.lpmfoundations.paymentmethod.IntegrationMetadata
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadataFactory
import com.stripe.android.model.Address
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.PaymentMethodFixtures
import com.stripe.android.networktesting.NetworkRule
import com.stripe.android.paymentelement.EmbeddedPaymentElement
import com.stripe.android.paymentelement.embedded.EmbeddedActivityArgs
import com.stripe.android.paymentelement.embedded.EmbeddedActivityResult
import com.stripe.android.paymentelement.embedded.EmbeddedLaunchMode
import com.stripe.android.paymentelement.embedded.stashNewSelection
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.PaymentSheetFixtures
import com.stripe.android.paymentsheet.R
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.repositories.CheckoutSessionResponse
import com.stripe.android.paymentsheet.repositories.CheckoutSessionResponseFactory
import com.stripe.android.paymentsheet.state.CustomerState
import com.stripe.android.paymentsheet.ui.PRIMARY_BUTTON_TEST_TAG
import com.stripe.android.paymentsheet.verticalmode.FakeSavedPaymentMethodConfirmInteractor
import com.stripe.android.paymentsheet.verticalmode.TEST_TAG_NEW_PAYMENT_METHOD_ROW_BUTTON
import com.stripe.android.testing.PaymentConfigurationTestRule
import com.stripe.paymentelementtestpages.ManagePage
import com.stripe.paymentelementtestpages.VerticalModePage
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class PaymentOptionsEmbeddedSheetActivityTest {
    private val applicationContext = ApplicationProvider.getApplicationContext<Application>()
    private val composeTestRule = createAndroidComposeRule<EmbeddedSheetActivity>()
    private val managePage = ManagePage(composeTestRule)
    private val verticalModePage = VerticalModePage(composeTestRule)
    private val networkRule = NetworkRule()

    @get:Rule
    val ruleChain: RuleChain = RuleChain
        .outerRule(composeTestRule)
        .around(networkRule)
        .around(PaymentConfigurationTestRule(applicationContext))

    @Test
    fun `pressing back returns cancelled result with PaymentOptions launch mode`() = launch { scenario ->
        Espresso.pressBack()

        onIdle()

        assertThat(scenario.result.resultCode).isEqualTo(Activity.RESULT_OK)
        val result = EmbeddedSheetContract.parseResult(scenario.result.resultCode, scenario.result.resultData)
        assertThat(result).isInstanceOf<EmbeddedActivityResult.Cancelled>()
        val cancelled = result as EmbeddedActivityResult.Cancelled
        assertThat(cancelled.launchMode).isEqualTo(
            EmbeddedLaunchMode.PaymentOptions
        )
    }

    @Test
    fun `when SheetActivityStateHolder has result, activity finishes with that result`() = launch { scenario ->
        scenario.onActivity { activity ->
            activity.sheetActivityStateHolder.setResult(
                EmbeddedActivityResult.Complete(
                    previousNewSelections = Bundle(),
                    selection = null,
                    hasBeenConfirmed = false,
                    customerState = null,
                    checkoutSessionResponse = null,
                    shouldInvokeSelectionCallback = false,
                    launchMode = EmbeddedLaunchMode.PaymentOptions,
                )
            )
        }

        onIdle()

        assertThat(scenario.result.resultCode).isEqualTo(Activity.RESULT_OK)
        val result = EmbeddedSheetContract.parseResult(scenario.result.resultCode, scenario.result.resultData)
        assertThat(result).isInstanceOf<EmbeddedActivityResult.Complete>()
        val complete = result as EmbeddedActivityResult.Complete
        assertThat(complete.launchMode).isEqualTo(
            EmbeddedLaunchMode.PaymentOptions
        )
    }

    @Test
    fun `navigator back emits cancelled result`() = launch { scenario ->
        scenario.onActivity { activity ->
            activity.embeddedNavigator.performAction(EmbeddedNavigator.Action.Back)
        }

        onIdle()

        assertThat(scenario.result.resultCode).isEqualTo(Activity.RESULT_OK)
        val result = EmbeddedSheetContract.parseResult(scenario.result.resultCode, scenario.result.resultData)
        assertThat(result).isInstanceOf<EmbeddedActivityResult.Cancelled>()
        val cancelled = result as EmbeddedActivityResult.Cancelled
        assertThat(cancelled.launchMode).isEqualTo(
            EmbeddedLaunchMode.PaymentOptions
        )
    }

    @Test
    fun `restores previously entered new selections into the selection holder`() = launch(
        previousNewSelections = Bundle().apply {
            stashNewSelection(PaymentMethodFixtures.CASHAPP_PAYMENT_SELECTION)
        },
    ) { scenario ->
        scenario.onActivity { activity ->
            assertThat(activity.selectionHolder.getPreviousNewSelection("cashapp"))
                .isEqualTo(PaymentMethodFixtures.CASHAPP_PAYMENT_SELECTION)
        }
    }

    @Test
    fun `new selection requiring a form opens on the form and back returns to the list`() = launch(
        selection = PaymentMethodFixtures.CARD_PAYMENT_SELECTION,
    ) { scenario ->
        scenario.onActivity { activity ->
            assertThat(activity.embeddedNavigator.canGoBack).isTrue()
            assertThat(activity.embeddedNavigator.screen.value)
                .isInstanceOf<EmbeddedNavigator.Screen.Form>()
        }

        Espresso.pressBack()
        onIdle()

        // Back returns to the payment options list rather than cancelling the sheet.
        scenario.onActivity { activity ->
            assertThat(activity.embeddedNavigator.canGoBack).isFalse()
            assertThat(activity.embeddedNavigator.screen.value)
                .isInstanceOf<EmbeddedNavigator.Screen.VerticalPaymentOptions>()
        }
    }

    @Test
    fun `selecting saved payment method from manage returns to payment options`() {
        val paymentMethods = PaymentMethodFixtures.createCards(2)

        launch(
            customerState = PaymentSheetFixtures.EMPTY_CUSTOMER_STATE.copy(
                paymentMethods = paymentMethods,
            ),
        ) { scenario ->
            verticalModePage.clickViewMore()
            managePage.waitUntilVisible()

            managePage.selectPaymentMethod(paymentMethods.first().id)
            composeTestRule.waitForIdle()

            managePage.assertNotVisible()
            verticalModePage.waitUntilVisible()
            scenario.onActivity { activity ->
                assertThat(activity.embeddedNavigator.canGoBack).isFalse()
                assertThat(activity.embeddedNavigator.screen.value)
                    .isInstanceOf<EmbeddedNavigator.Screen.VerticalPaymentOptions>()
            }
        }
    }

    @Test
    fun `configuration change keeps saved payment method confirmation screen`() = launch { scenario ->
        val interactor = FakeSavedPaymentMethodConfirmInteractor()
        lateinit var originalScreen: EmbeddedNavigator.Screen.SavedPaymentMethodConfirm
        scenario.onActivity { activity ->
            originalScreen = EmbeddedNavigator.Screen.SavedPaymentMethodConfirm(
                interactor = interactor,
                isLiveMode = true,
                sheetActivityStateHolder = activity.sheetActivityStateHolder,
                confirmationHelper = FakeSheetActivityConfirmationHelper(),
                embeddedSelectionHolder = activity.selectionHolder,
                customerStateHolder = activity.customerStateHolder,
                launchMode = EmbeddedLaunchMode.PaymentOptions,
            )
            activity.embeddedNavigator.performAction(
                EmbeddedNavigator.Action.ReplaceCurrentScreen(originalScreen)
            )
        }
        onIdle()

        scenario.recreate()
        onIdle()

        scenario.onActivity { activity ->
            assertThat(activity.embeddedNavigator.screen.value).isSameInstanceAs(originalScreen)
        }
        interactor.validate()
    }

    @Test
    fun `cancelled result contains customer state`() = launch { scenario ->
        Espresso.pressBack()

        onIdle()

        val result = EmbeddedSheetContract.parseResult(scenario.result.resultCode, scenario.result.resultData)
        val cancelled = result as EmbeddedActivityResult.Cancelled
        assertThat(cancelled.customerState).isNotNull()
    }

    @Test
    fun `processing blocks back and disables vertical payment method rows`() = launch { scenario ->
        val cardRow = composeTestRule.onNodeWithTag("${TEST_TAG_NEW_PAYMENT_METHOD_ROW_BUTTON}_card")
        cardRow.assertIsEnabled()

        scenario.onActivity { activity ->
            activity.sheetActivityStateHolder.updateProcessing(true)
        }
        composeTestRule.waitForIdle()

        cardRow.assertIsNotEnabled()
        composeTestRule.onNodeWithText(
            applicationContext.getString(R.string.stripe_paymentsheet_primary_button_processing)
        ).assertIsDisplayed()
        Espresso.pressBack()
        onIdle()
        scenario.onActivity { activity ->
            assertThat(activity.embeddedNavigator.screen.value)
                .isInstanceOf<EmbeddedNavigator.Screen.VerticalPaymentOptions>()
        }
    }

    @Test
    fun `vertical payment options displays activity error`() = launch { scenario ->
        val errorMessage = "Unable to update the tax region."

        scenario.onActivity { activity ->
            activity.sheetActivityStateHolder.updateError(errorMessage.resolvableString)
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText(errorMessage).assertIsDisplayed()
    }

    @Test
    fun `saved checkout selection displays update error and re-enables payment options`() {
        val selection = savedSelectionWithBillingAddress()
        networkRule.checkoutUpdate { response ->
            response.setResponseCode(400)
            response.setBody("""{"error":{"message":"Invalid tax region"}}""")
        }

        launch(
            selection = selection,
            paymentMethodMetadata = checkoutPaymentMethodMetadata(),
        ) { scenario ->
            val primaryButton = composeTestRule.onNodeWithTag(PRIMARY_BUTTON_TEST_TAG)
            val expectedError = applicationContext.getString(R.string.stripe_something_went_wrong)
            primaryButton.performScrollTo().assertIsDisplayed().assertIsEnabled().performClick()

            composeTestRule.waitUntil(timeoutMillis = 5_000) {
                composeTestRule.onAllNodesWithText(expectedError)
                    .fetchSemanticsNodes(atLeastOneRootRequired = false)
                    .isNotEmpty()
            }
            composeTestRule.onNodeWithText(expectedError).performScrollTo().assertIsDisplayed()
            primaryButton.assertIsEnabled()
            composeTestRule.onNodeWithTag("${TEST_TAG_NEW_PAYMENT_METHOD_ROW_BUTTON}_card").assertIsEnabled()
            scenario.onActivity { activity ->
                assertThat(activity.embeddedNavigator.screen.value)
                    .isInstanceOf<EmbeddedNavigator.Screen.VerticalPaymentOptions>()
                assertThat(activity.sheetActivityStateHolder.state.value.isProcessing).isFalse()
            }
        }
    }

    private fun launch(
        selection: PaymentSelection? = null,
        previousNewSelections: Bundle = Bundle(),
        paymentMethodMetadata: PaymentMethodMetadata = PaymentMethodMetadataFactory.create(
            paymentMethodLayout = PaymentSheet.PaymentMethodLayout.Vertical,
        ),
        block: (ActivityScenario<EmbeddedSheetActivity>) -> Unit,
    ) = launch(
        selection = selection,
        previousNewSelections = previousNewSelections,
        paymentMethodMetadata = paymentMethodMetadata,
        customerState = PaymentSheetFixtures.EMPTY_CUSTOMER_STATE.copy(
            paymentMethods = (selection as? PaymentSelection.Saved)?.let {
                listOf(it.paymentMethod)
            }.orEmpty(),
        ),
        block = block,
    )

    private fun launch(
        customerState: CustomerState,
        block: (ActivityScenario<EmbeddedSheetActivity>) -> Unit,
    ) = launch(
        selection = null,
        previousNewSelections = Bundle(),
        paymentMethodMetadata = PaymentMethodMetadataFactory.create(
            paymentMethodLayout = PaymentSheet.PaymentMethodLayout.Vertical,
        ),
        customerState = customerState,
        block = block,
    )

    private fun launch(
        selection: PaymentSelection?,
        previousNewSelections: Bundle,
        paymentMethodMetadata: PaymentMethodMetadata,
        customerState: CustomerState,
        block: (ActivityScenario<EmbeddedSheetActivity>) -> Unit,
    ) {
        ActivityScenario.launchActivityForResult<EmbeddedSheetActivity>(
            EmbeddedSheetContract.createIntent(
                context = applicationContext,
                input = EmbeddedActivityArgs(
                    paymentMethodMetadata = paymentMethodMetadata,
                    configuration = EmbeddedPaymentElement.Configuration.Builder("Example, Inc.").build(),
                    productUsage = setOf("EmbeddedPaymentElement"),
                    statusBarColor = null,
                    paymentElementCallbackIdentifier = "PaymentOptionsTestIdentifier",
                    selection = selection,
                    previousNewSelections = previousNewSelections,
                    customerState = customerState,
                    promotion = null,
                    launchMode = EmbeddedLaunchMode.PaymentOptions,
                ),
            )
        ).use { scenario ->
            block(scenario)
        }
    }

    private fun checkoutPaymentMethodMetadata(): PaymentMethodMetadata {
        val response = CheckoutSessionResponseFactory.create(
            automaticTaxEnabled = true,
            taxAddressSource = CheckoutSessionResponse.TaxAddressSource.BILLING,
        )
        return PaymentMethodMetadataFactory.create(
            integrationMetadata = IntegrationMetadata.CheckoutSession(
                id = response.id,
                instancesKey = "test_instances_key",
                checkoutSessionResponse = response,
            ),
            paymentMethodLayout = PaymentSheet.PaymentMethodLayout.Vertical,
        )
    }

    private fun savedSelectionWithBillingAddress(): PaymentSelection.Saved {
        return PaymentSelection.Saved(
            PaymentMethodFixtures.CARD_PAYMENT_METHOD.copy(
                billingDetails = PaymentMethod.BillingDetails(
                    address = Address(
                        city = "San Francisco",
                        country = "US",
                        line1 = "510 Townsend St",
                        line2 = "Suite 100",
                        postalCode = "94103",
                        state = "CA",
                    ),
                ),
            )
        )
    }
}
