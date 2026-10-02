package com.stripe.android.paymentsheet.addresselement

import android.app.Activity
import android.app.Application
import android.content.Intent
import android.os.Bundle
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextReplacement
import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.stripe.android.checkouttesting.checkoutUpdate
import com.stripe.android.common.ui.PRIMARY_BUTTON_LOADING_INDICATOR_TEST_TAG
import com.stripe.android.core.utils.FeatureFlags
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadataFixtures.DEFAULT_API_CONFIG
import com.stripe.android.networktesting.NetworkRule
import com.stripe.android.networktesting.testBodyFromFile
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.R
import com.stripe.android.paymentsheet.repositories.CheckoutSessionResponse
import com.stripe.android.paymentsheet.repositories.CheckoutSessionResponseFactory
import com.stripe.android.testing.CoroutineTestRule
import com.stripe.android.testing.FeatureFlagTestRule
import com.stripe.android.testing.createComposeCleanupRule
import com.stripe.android.testing.waitUntilWithIdle
import com.stripe.android.uicore.elements.bottomsheet.BottomSheetContentTestTag
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import com.stripe.android.core.R as CoreR

@RunWith(RobolectricTestRunner::class)
internal class AddressElementActivityTest {
    private val applicationContext = ApplicationProvider.getApplicationContext<Application>()
    private val composeTestRule = createEmptyComposeRule()
    private val networkRule = NetworkRule()
    private val unsavedChangesFeatureFlagRule = FeatureFlagTestRule(
        featureFlag = FeatureFlags.enableAddressElementUnsavedChanges,
        isEnabled = true,
    )

    @get:Rule
    val ruleChain: RuleChain = RuleChain
        .outerRule(composeTestRule)
        .around(networkRule)
        .around(createComposeCleanupRule())
        .around(CoroutineTestRule())
        .around(unsavedChangesFeatureFlagRule)

    @Test
    fun `when launched without args should finish with canceled result`() {
        ActivityScenario.launchActivityForResult(
            AddressElementActivity::class.java,
            Bundle.EMPTY
        ).use { activityScenario ->
            assertThat(activityScenario.state).isEqualTo(Lifecycle.State.DESTROYED)
            val result = AddressElementActivityContract.Standalone.parseResult(
                activityScenario.result.resultCode,
                activityScenario.result.resultData,
            )
            assertThat(result).isEqualTo(AddressLauncherResult.Canceled())
        }
    }

    @Test
    fun `standalone contract creates intent with standalone args`() {
        val args = AddressElementActivityContract.Args.Standalone(
            apiConfiguration = DEFAULT_API_CONFIG,
            config = null,
        )

        val intent = AddressElementActivityContract.Standalone.createIntent(
            ApplicationProvider.getApplicationContext(),
            args,
        )

        assertThat(intent.component?.className).isEqualTo(AddressElementActivity::class.java.name)
        assertThat(AddressElementActivityContract.Args.fromIntent(intent)).isEqualTo(args)
    }

    @Test
    fun `checkout shipping contract creates intent with checkout shipping args`() {
        val args = AddressElementActivityContract.Args.CheckoutShipping(
            apiConfiguration = DEFAULT_API_CONFIG,
            config = null,
            checkoutSessionResponse = CheckoutSessionResponseFactory.create(),
        )

        val intent = AddressElementActivityContract.CheckoutShipping.createIntent(
            ApplicationProvider.getApplicationContext(),
            args,
        )

        assertThat(intent.component?.className).isEqualTo(AddressElementActivity::class.java.name)
        assertThat(AddressElementActivityContract.Args.fromIntent(intent)).isEqualTo(args)
    }

    @Test
    fun `standalone contract maps standalone success to public success`() {
        val result = AddressElementActivityContract.Result.StandaloneSucceeded(AddressDetails())

        val parsed = AddressElementActivityContract.Standalone.parseResult(
            resultCode = result.resultCode,
            intent = Intent().putExtras(result.toBundle()),
        )

        assertThat(parsed).isEqualTo(AddressLauncherResult.Succeeded(result.address))
    }

    @Test
    fun `standalone contract maps missing result to canceled`() {
        val parsed = AddressElementActivityContract.Standalone.parseResult(
            resultCode = Activity.RESULT_CANCELED,
            intent = Intent(),
        )

        assertThat(parsed).isEqualTo(AddressLauncherResult.Canceled())
    }

    @Test
    fun `standalone contract maps canceled result to public canceled`() {
        val result = AddressElementActivityContract.Result.Canceled

        val parsed = AddressElementActivityContract.Standalone.parseResult(
            resultCode = result.resultCode,
            intent = Intent().putExtras(result.toBundle()),
        )

        assertThat(parsed).isEqualTo(AddressLauncherResult.Canceled())
    }

    @Test
    fun `standalone contract maps checkout shipping success to canceled`() {
        val result = AddressElementActivityContract.Result.CheckoutShippingSucceeded(
            address = AddressDetails(),
            checkoutSessionResponse = CheckoutSessionResponseFactory.create(),
        )

        val parsed = AddressElementActivityContract.Standalone.parseResult(
            resultCode = result.resultCode,
            intent = Intent().putExtras(result.toBundle()),
        )

        assertThat(parsed).isEqualTo(AddressLauncherResult.Canceled())
    }

    @Test
    fun `checkout shipping contract preserves checkout shipping success`() {
        val result = AddressElementActivityContract.Result.CheckoutShippingSucceeded(
            address = AddressDetails(),
            checkoutSessionResponse = CheckoutSessionResponseFactory.create(amount = 2000L),
        )

        val parsed = AddressElementActivityContract.CheckoutShipping.parseResult(
            resultCode = result.resultCode,
            intent = Intent().putExtras(result.toBundle()),
        )

        assertThat(parsed).isEqualTo(result)
    }

    @Test
    fun `checkout shipping contract maps missing result to canceled`() {
        val parsed = AddressElementActivityContract.CheckoutShipping.parseResult(
            resultCode = Activity.RESULT_CANCELED,
            intent = Intent(),
        )

        assertThat(parsed).isEqualTo(AddressElementActivityContract.Result.Canceled)
    }

    @Test
    fun `checkout shipping contract preserves canceled result`() {
        val result = AddressElementActivityContract.Result.Canceled

        val parsed = AddressElementActivityContract.CheckoutShipping.parseResult(
            resultCode = result.resultCode,
            intent = Intent().putExtras(result.toBundle()),
        )

        assertThat(parsed).isEqualTo(result)
    }

    @Test
    fun `checkout shipping contract maps standalone success to canceled`() {
        val result = AddressElementActivityContract.Result.StandaloneSucceeded(AddressDetails())

        val parsed = AddressElementActivityContract.CheckoutShipping.parseResult(
            resultCode = result.resultCode,
            intent = Intent().putExtras(result.toBundle()),
        )

        assertThat(parsed).isEqualTo(AddressElementActivityContract.Result.Canceled)
    }

    @Test
    fun `standalone X and root back keep edited input after cancel and discard returns canceled`() = runScenario(
        args = standaloneArgs(SHIPPING_ADDRESS),
    ) {
        page.name.performTextReplacement(EDITED_NAME)
        page.close.performClick()
        page.discardConfirmation.assertIsDisplayed()

        page.cancel.performClick()

        page.discardConfirmation.assertDoesNotExist()
        page.name.assertIsDisplayed().assertTextContains(EDITED_NAME)
        activityScenario.onActivity { activity ->
            activity.onBackPressedDispatcher.onBackPressed()
        }
        page.discardConfirmation.assertIsDisplayed()

        page.cancel.performClick()

        page.discardConfirmation.assertDoesNotExist()
        page.name.assertIsDisplayed().assertTextContains(EDITED_NAME)
        page.close.performClick()
        page.discard.performClick()

        assertFinished()
        assertThat(
            AddressElementActivityContract.Standalone.parseResult(
                activityScenario.result.resultCode,
                activityScenario.result.resultData,
            )
        ).isEqualTo(AddressLauncherResult.Canceled())
    }

    @Test
    fun `checkout shipping X keeps edited input after cancel and discard returns canceled`() = runScenario(
        args = checkoutShippingArgs(SHIPPING_ADDRESS),
    ) {
        page.name.performTextReplacement(EDITED_NAME)
        page.close.performClick()
        page.discardConfirmation.assertIsDisplayed()

        page.cancel.performClick()

        page.discardConfirmation.assertDoesNotExist()
        page.name.assertIsDisplayed().assertTextContains(EDITED_NAME)
        page.close.performClick()
        page.discard.performClick()

        assertFinished()
        assertThat(
            AddressElementActivityContract.CheckoutShipping.parseResult(
                activityScenario.result.resultCode,
                activityScenario.result.resultData,
            )
        ).isEqualTo(AddressElementActivityContract.Result.Canceled)
    }

    @Test
    fun `standalone X immediately dismisses unchanged prefilled input`() = runScenario(
        args = standaloneArgs(SHIPPING_ADDRESS),
    ) {
        page.name.assertTextContains("Jenny Rosen")

        page.close.performClick()

        assertFinished()
        assertThat(activityScenario.result.resultCode).isEqualTo(Activity.RESULT_CANCELED)
    }

    @Test
    fun `checkout shipping X immediately dismisses unchanged name-only input`() = runScenario(
        args = checkoutShippingArgs(AddressDetails(name = "Jenny Rosen")),
    ) {
        page.name.assertTextContains("Jenny Rosen")

        page.close.performClick()

        assertFinished()
        assertThat(activityScenario.result.resultCode).isEqualTo(Activity.RESULT_CANCELED)
    }

    @Test
    fun `standalone X immediately dismisses edited input when feature flag is disabled`() {
        unsavedChangesFeatureFlagRule.setEnabled(false)
        runScenario(args = standaloneArgs(SHIPPING_ADDRESS)) {
            page.name.performTextReplacement(EDITED_NAME)
            page.name.assertTextContains(EDITED_NAME)

            page.close.performClick()

            assertFinished()
            assertThat(activityScenario.result.resultCode).isEqualTo(Activity.RESULT_CANCELED)
        }
    }

    @Test
    fun `checkout shipping X cannot dismiss while tax update is in flight and save can finish`() {
        val requestReceived = CountDownLatch(1)
        val releaseResponse = CountDownLatch(1)
        networkRule.checkoutUpdate { response ->
            response.testBodyFromFile("checkout-session-init.json")
            requestReceived.countDown()
            check(releaseResponse.await(10, TimeUnit.SECONDS))
        }

        ActivityScenario.launchActivityForResult<AddressElementActivity>(
            AddressElementActivityContract.CheckoutShipping.createIntent(
                applicationContext,
                AddressElementActivityContract.Args.CheckoutShipping(
                    apiConfiguration = DEFAULT_API_CONFIG,
                    config = AddressLauncher.Configuration.Builder()
                        .address(SHIPPING_ADDRESS)
                        .build(),
                    checkoutSessionResponse = CheckoutSessionResponseFactory.create(
                        automaticTaxEnabled = true,
                        taxAddressSource = CheckoutSessionResponse.TaxAddressSource.SHIPPING,
                    ),
                ),
            )
        ).use { activityScenario ->
            try {
                val page = Page()
                page.name.performTextReplacement(EDITED_NAME)
                val primaryButton = composeTestRule.onNodeWithText(
                    applicationContext.getString(R.string.stripe_paymentsheet_address_element_primary_button)
                )
                primaryButton
                    .performScrollTo()
                    .assertIsEnabled()
                    .performClick()
                composeTestRule.waitUntilWithIdle {
                    requestReceived.count == 0L
                }
                primaryButton.assertIsNotEnabled()
                composeTestRule.onNodeWithTag(PRIMARY_BUTTON_LOADING_INDICATOR_TEST_TAG, useUnmergedTree = true)
                    .assertIsDisplayed()
                page.close.performClick()

                composeTestRule.onNodeWithTag(BottomSheetContentTestTag).assertIsDisplayed()
                page.discardConfirmation.assertDoesNotExist()
                assertThat(activityScenario.state).isEqualTo(Lifecycle.State.RESUMED)
            } finally {
                releaseResponse.countDown()
            }
            composeTestRule.waitUntilWithIdle {
                activityScenario.state == Lifecycle.State.DESTROYED
            }
            assertThat(
                AddressElementActivityContract.CheckoutShipping.parseResult(
                    activityScenario.result.resultCode,
                    activityScenario.result.resultData,
                )
            ).isInstanceOf(AddressElementActivityContract.Result.CheckoutShippingSucceeded::class.java)
        }
    }

    private fun runScenario(
        args: AddressElementActivityContract.Args,
        block: Scenario.() -> Unit,
    ) {
        val intent = when (args) {
            is AddressElementActivityContract.Args.Standalone -> {
                AddressElementActivityContract.Standalone.createIntent(applicationContext, args)
            }
            is AddressElementActivityContract.Args.CheckoutShipping -> {
                AddressElementActivityContract.CheckoutShipping.createIntent(applicationContext, args)
            }
        }
        ActivityScenario.launchActivityForResult<AddressElementActivity>(intent).use { activityScenario ->
            val page = Page()
            page.name.assertIsDisplayed()
            block(Scenario(activityScenario = activityScenario, page = page))
        }
    }

    private fun standaloneArgs(address: AddressDetails) = AddressElementActivityContract.Args.Standalone(
        apiConfiguration = DEFAULT_API_CONFIG,
        config = AddressLauncher.Configuration.Builder().address(address).build(),
    )

    private fun checkoutShippingArgs(address: AddressDetails) = AddressElementActivityContract.Args.CheckoutShipping(
        apiConfiguration = DEFAULT_API_CONFIG,
        config = AddressLauncher.Configuration.Builder().address(address).build(),
        checkoutSessionResponse = CheckoutSessionResponseFactory.create(),
    )

    private inner class Scenario(
        val activityScenario: ActivityScenario<AddressElementActivity>,
        val page: Page,
    ) {
        fun assertFinished() {
            composeTestRule.waitUntilWithIdle {
                activityScenario.state == Lifecycle.State.DESTROYED
            }
        }
    }

    private inner class Page {
        val name = composeTestRule.onNode(
            hasText(applicationContext.getString(CoreR.string.stripe_address_label_full_name))
                .and(hasSetTextAction())
        )
        val close = composeTestRule.onNodeWithContentDescription(
            applicationContext.getString(R.string.stripe_paymentsheet_close)
        )
        val discardConfirmation = composeTestRule.onNodeWithText(
            applicationContext.getString(R.string.stripe_paymentsheet_address_element_discard_changes_title)
        )
        val cancel = composeTestRule.onNodeWithText(applicationContext.getString(android.R.string.cancel))
        val discard = composeTestRule.onNodeWithText(
            applicationContext.getString(R.string.stripe_paymentsheet_address_element_discard_changes_confirm)
        )
    }

    private companion object {
        const val EDITED_NAME = "Edited Name"
        val SHIPPING_ADDRESS = AddressDetails(
            name = "Jenny Rosen",
            address = PaymentSheet.Address(
                city = "San Francisco",
                country = "US",
                line1 = "510 Townsend St",
                postalCode = "94103",
                state = "CA",
            ),
        )
    }
}
