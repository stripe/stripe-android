package com.stripe.android.paymentsheet.addresselement

import android.app.Activity
import android.app.Application
import android.content.Intent
import android.os.Bundle
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadataFixtures.DEFAULT_API_CONFIG
import com.stripe.android.networktesting.NetworkRule
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.R
import com.stripe.android.paymentsheet.repositories.CheckoutSessionResponse
import com.stripe.android.paymentsheet.repositories.CheckoutSessionResponseFactory
import com.stripe.android.testing.createComposeCleanupRule
import com.stripe.android.ui.core.elements.TEST_TAG_DIALOG_CONFIRM_BUTTON
import com.stripe.android.ui.core.elements.TEST_TAG_DIALOG_DISMISS_BUTTON
import com.stripe.android.ui.core.elements.TEST_TAG_SIMPLE_DIALOG
import com.stripe.paymentelementtestpages.AddressElementPage
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class AddressElementActivityTest {
    private val applicationContext = ApplicationProvider.getApplicationContext<Application>()
    private val composeTestRule = createEmptyComposeRule()
    private val networkRule = NetworkRule()
    private val addressPage = AddressElementPage(
        composeTestRule = composeTestRule,
        context = applicationContext,
    )
    private val activityTestRunner = AddressElementActivityTestRunner(composeTestRule, networkRule, addressPage)

    @get:Rule
    val ruleChain: RuleChain = RuleChain
        .outerRule(composeTestRule)
        .around(createComposeCleanupRule())
        .around(networkRule)

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
    fun `standalone shipping returns the entered address without a tax update`() = runScenario(
        args = AddressElementActivityContract.Args.Standalone(
            apiConfiguration = DEFAULT_API_CONFIG,
            config = AddressLauncher.Configuration.Builder()
                .address(SHIPPING_ADDRESS)
                .build(),
        ),
    ) {
        addressPage.assertReadyToSave()
        addressPage.editName("Jenny Rosen Updated")
        addressPage.clickSave()

        val result = awaitStandaloneResult() as AddressLauncherResult.Succeeded
        assertThat(result.address.name).isEqualTo("Jenny Rosen Updated")
        assertThat(result.address.address?.country).isEqualTo(SHIPPING_ADDRESS.address?.country)
        assertThat(result.address.address?.line1).isEqualTo(SHIPPING_ADDRESS.address?.line1)
        assertThat(result.address.address?.postalCode).isEqualTo(SHIPPING_ADDRESS.address?.postalCode)
    }

    @Test
    fun `standalone shipping can be canceled with close before saving`() = runScenario(
        args = AddressElementActivityContract.Args.Standalone(
            apiConfiguration = DEFAULT_API_CONFIG,
            config = AddressLauncher.Configuration.Builder()
                .address(SHIPPING_ADDRESS)
                .build(),
        ),
    ) {
        addressPage.assertReadyToSave()
        addressPage.clickClose()

        assertThat(awaitStandaloneResult()).isEqualTo(AddressLauncherResult.Canceled())
    }

    @Test
    fun `checkout shipping shows loading during tax update and returns success`() = runScenario {
        val taxUpdate = enqueueTaxUpdate()

        try {
            startTaxUpdate(taxUpdate)
            assertSaving()

            taxUpdate.releaseResponse.countDown()

            val result = awaitResult() as AddressElementActivityContract.Result.CheckoutShippingSucceeded
            assertThat(result.address.name).isEqualTo(SHIPPING_ADDRESS.name)
            assertThat(result.address.address?.country).isEqualTo(SHIPPING_ADDRESS.address?.country)
            assertThat(result.address.address?.line1).isEqualTo(SHIPPING_ADDRESS.address?.line1)
            assertThat(result.address.address?.postalCode).isEqualTo(SHIPPING_ADDRESS.address?.postalCode)
            assertThat(result.checkoutSessionResponse.id).isEqualTo(checkoutSessionResponse.id)
            assertThat(result.checkoutSessionResponse.amount).isEqualTo(5099L)
        } finally {
            taxUpdate.releaseResponse.countDown()
        }
    }

    @Test
    fun `checkout shipping displays save error and clears it after editing`() = runScenario {
        val expectedError = applicationContext.getString(R.string.stripe_something_went_wrong)
        addressPage.assertReadyToSave()
        addressPage.assertErrorNotDisplayed(expectedError)
        val taxUpdate = enqueueTaxUpdate(fails = true)

        try {
            startTaxUpdate(taxUpdate)
            assertSaving()
            addressPage.assertCloseDisabled()
            taxUpdate.releaseResponse.countDown()

            addressPage.assertErrorDisplayed(expectedError)
            addressPage.assertReadyToSave()
            addressPage.editName("Jenny Rosen Updated")
            addressPage.assertErrorNotDisplayed(expectedError)
            addressPage.assertReadyToSave()
            addressPage.clickClose()
            assertDiscardConfirmation()
            discardChanges()

            assertThat(awaitResult()).isEqualTo(AddressElementActivityContract.Result.Canceled)
        } finally {
            taxUpdate.releaseResponse.countDown()
        }
    }

    @Test
    fun `checkout shipping can be canceled with back before saving`() = runScenario {
        addressPage.assertReadyToSave()

        activityScenario.onActivity { activity ->
            activity.onBackPressedDispatcher.onBackPressed()
        }

        assertThat(awaitResult()).isEqualTo(AddressElementActivityContract.Result.Canceled)
    }

    @Test
    fun `checkout shipping can be canceled with scrim accessibility action before saving`() = runScenario {
        addressPage.assertReadyToSave()

        addressPage.dismissViaScrimAccessibilityAction()

        assertThat(awaitResult()).isEqualTo(AddressElementActivityContract.Result.Canceled)
    }

    @Test
    fun `checkout shipping close is disabled while tax update is in flight`() = runScenario {
        val taxUpdate = enqueueTaxUpdate()

        try {
            startTaxUpdate(taxUpdate)
            assertSaving()
            addressPage.assertCloseDisabled()

            addressPage.clickClose()
            assertSaving()
            addressPage.assertCloseDisabled()

            taxUpdate.releaseResponse.countDown()
            awaitResult()
        } finally {
            taxUpdate.releaseResponse.countDown()
        }
    }

    @Test
    fun `checkout shipping back does not dismiss while tax update is in flight`() = runScenario {
        val taxUpdate = enqueueTaxUpdate()

        try {
            startTaxUpdate(taxUpdate)
            assertSaving()

            activityScenario.onActivity { activity ->
                activity.onBackPressedDispatcher.onBackPressed()
            }
            assertSaving()

            taxUpdate.releaseResponse.countDown()
            awaitResult()
        } finally {
            taxUpdate.releaseResponse.countDown()
        }
    }

    @Test
    fun `checkout shipping scrim accessibility action does not dismiss while tax update is in flight`() = runScenario {
        val taxUpdate = enqueueTaxUpdate()

        try {
            startTaxUpdate(taxUpdate)
            assertSaving()

            addressPage.dismissViaScrimAccessibilityAction()
            assertSaving()

            taxUpdate.releaseResponse.countDown()
            awaitResult()
        } finally {
            taxUpdate.releaseResponse.countDown()
        }
    }

    @Test
    fun `checkout shipping keeps save and close disabled after recreation during tax update`() = runScenario {
        val taxUpdate = enqueueTaxUpdate()

        try {
            startTaxUpdate(taxUpdate)
            assertSaving()
            addressPage.assertCloseDisabled()

            activityScenario.recreate()
            activityScenario.onActivity { activity = it }
            composeTestRule.waitForIdle()

            addressPage.assertVisible()
            assertSaving()
            addressPage.assertCloseDisabled()

            taxUpdate.releaseResponse.countDown()

            val result = awaitResult() as AddressElementActivityContract.Result.CheckoutShippingSucceeded
            assertThat(result.address.name).isEqualTo(SHIPPING_ADDRESS.name)
            assertThat(result.address.address?.line1).isEqualTo(SHIPPING_ADDRESS.address?.line1)
            assertThat(result.checkoutSessionResponse.id).isEqualTo(checkoutSessionResponse.id)
            assertThat(result.checkoutSessionResponse.amount).isEqualTo(5099L)
        } finally {
            taxUpdate.releaseResponse.countDown()
        }
    }

    @Test
    fun `standalone close confirms edited input and cancel preserves the form`() = runScenario(
        args = standaloneArgs(SHIPPING_ADDRESS),
    ) {
        assertDirtyDismissal { addressPage.clickClose() }
    }

    @Test
    fun `standalone root back confirms edited input and cancel preserves the form`() = runScenario(
        args = standaloneArgs(SHIPPING_ADDRESS),
    ) {
        assertDirtyDismissal { activityScenario.onActivity { it.onBackPressedDispatcher.onBackPressed() } }
    }

    @Test
    fun `standalone scrim confirms edited input and cancel preserves the form`() = runScenario(
        args = standaloneArgs(SHIPPING_ADDRESS),
    ) {
        assertDirtyDismissal { addressPage.dismissViaScrimAccessibilityAction() }
    }

    @Test
    fun `checkout shipping close confirms edited input and cancel preserves the form`() = runScenario {
        assertDirtyDismissal { addressPage.clickClose() }
    }

    @Test
    fun `checkout shipping root back confirms edited input and cancel preserves the form`() = runScenario {
        assertDirtyDismissal { activityScenario.onActivity { it.onBackPressedDispatcher.onBackPressed() } }
    }

    @Test
    fun `checkout shipping scrim confirms edited input and cancel preserves the form`() = runScenario {
        assertDirtyDismissal { addressPage.dismissViaScrimAccessibilityAction() }
    }

    @Test
    fun `checkout shipping unchanged name-only input dismisses immediately`() = runScenario(
        args = AddressElementActivityContract.Args.CheckoutShipping(
            apiConfiguration = DEFAULT_API_CONFIG,
            config = AddressLauncher.Configuration.Builder().address(AddressDetails(name = "Jenny Rosen")).build(),
            checkoutSessionResponse = CheckoutSessionResponseFactory.create(),
        ),
    ) {
        assertName("Jenny Rosen")
        addressPage.clickClose()

        assertThat(awaitResult()).isEqualTo(AddressElementActivityContract.Result.Canceled)
    }

    @Test
    fun `standalone edited input still requires confirmation after recreation`() = runScenario(
        args = standaloneArgs(SHIPPING_ADDRESS),
    ) {
        addressPage.editName(EDITED_NAME)
        activityScenario.recreate()
        activityScenario.onActivity { activity = it }
        composeTestRule.waitForIdle()
        assertName(EDITED_NAME)

        addressPage.clickClose()
        assertDiscardConfirmation()
        discardChanges()

        assertThat(awaitStandaloneResult()).isEqualTo(AddressLauncherResult.Canceled())
    }

    @Test
    fun `standalone discard confirmation survives recreation and cancel preserves input`() = runScenario(
        args = standaloneArgs(SHIPPING_ADDRESS),
    ) {
        addressPage.editName(EDITED_NAME)
        addressPage.clickClose()
        assertDiscardConfirmation()

        activityScenario.recreate()
        activityScenario.onActivity { activity = it }
        composeTestRule.waitForIdle()

        assertDiscardConfirmation()
        composeTestRule.onNodeWithTag(TEST_TAG_DIALOG_DISMISS_BUTTON).performClick()
        composeTestRule.onNodeWithTag(TEST_TAG_SIMPLE_DIALOG).assertDoesNotExist()
        assertName(EDITED_NAME)

        addressPage.clickClose()
        assertDiscardConfirmation()
        discardChanges()

        assertThat(awaitStandaloneResult()).isEqualTo(AddressLauncherResult.Canceled())
    }

    @Test
    fun `checkout shipping discard dialog survives recreation`() = runScenario {
        addressPage.assertReadyToSave()
        addressPage.editName(EDITED_NAME)
        addressPage.clickClose()
        assertDiscardConfirmation()

        activityScenario.recreate()
        activityScenario.onActivity { activity = it }
        composeTestRule.waitForIdle()

        assertDiscardConfirmation()
        keepEditing()
        assertDiscardConfirmationNotDisplayed()
        assertName(EDITED_NAME)
        addressPage.assertReadyToSave()

        addressPage.clickClose()
        assertDiscardConfirmation()
        discardChanges()

        assertThat(awaitResult()).isEqualTo(AddressElementActivityContract.Result.Canceled)
    }

    private fun AddressElementActivityTestRunner.Scenario.assertDirtyDismissal(requestDismissal: () -> Unit) {
        addressPage.assertReadyToSave()
        addressPage.editName(EDITED_NAME)
        requestDismissal()
        assertDiscardConfirmation()
        assertThat(activityScenario.state).isEqualTo(Lifecycle.State.RESUMED)

        composeTestRule.onNodeWithTag(TEST_TAG_DIALOG_DISMISS_BUTTON).performClick()
        composeTestRule.onNodeWithTag(TEST_TAG_SIMPLE_DIALOG).assertDoesNotExist()
        assertName(EDITED_NAME)
        addressPage.assertReadyToSave()

        requestDismissal()
        assertDiscardConfirmation()
        discardChanges()

        when (args) {
            is AddressElementActivityContract.Args.Standalone ->
                assertThat(awaitStandaloneResult()).isEqualTo(AddressLauncherResult.Canceled())
            is AddressElementActivityContract.Args.CheckoutShipping ->
                assertThat(awaitResult()).isEqualTo(AddressElementActivityContract.Result.Canceled)
        }
    }

    private fun assertDiscardConfirmation() {
        composeTestRule.onNodeWithTag(TEST_TAG_SIMPLE_DIALOG).assertIsDisplayed()
    }

    private fun assertDiscardConfirmationNotDisplayed() {
        composeTestRule.onNodeWithTag(TEST_TAG_SIMPLE_DIALOG).assertDoesNotExist()
    }

    private fun keepEditing() {
        composeTestRule.onNodeWithTag(TEST_TAG_DIALOG_DISMISS_BUTTON).performClick()
    }

    private fun discardChanges() {
        composeTestRule.onNodeWithTag(TEST_TAG_DIALOG_CONFIRM_BUTTON).performClick()
    }

    private fun assertName(name: String) {
        composeTestRule.onNode(hasText("Full name").and(hasSetTextAction()))
            .performScrollTo().assertTextContains(name)
    }

    private fun standaloneArgs(address: AddressDetails) = AddressElementActivityContract.Args.Standalone(
        apiConfiguration = DEFAULT_API_CONFIG,
        config = AddressLauncher.Configuration.Builder().address(address).build(),
    )

    private fun runScenario(
        args: AddressElementActivityContract.Args = AddressElementActivityContract.Args.CheckoutShipping(
            apiConfiguration = DEFAULT_API_CONFIG,
            config = AddressLauncher.Configuration.Builder()
                .address(SHIPPING_ADDRESS)
                .build(),
            checkoutSessionResponse = CheckoutSessionResponseFactory.create(
                automaticTaxEnabled = true,
                taxAddressSource = CheckoutSessionResponse.TaxAddressSource.SHIPPING,
            ),
        ),
        block: AddressElementActivityTestRunner.Scenario.() -> Unit,
    ) = activityTestRunner.run(args, block)

    private companion object {
        const val EDITED_NAME = "Jenny Rosen Updated"
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
