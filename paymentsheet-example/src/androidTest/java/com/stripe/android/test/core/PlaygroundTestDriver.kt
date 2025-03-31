package com.stripe.android.test.core

import android.app.Activity
import android.app.Application
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.lifecycle.lifecycleScope
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso
import androidx.test.espresso.Espresso.closeSoftKeyboard
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.IdlingPolicies
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.UiSelector
import com.google.common.truth.Truth.assertThat
import com.karumi.shot.ScreenshotTest
import com.stripe.android.customersheet.ui.CUSTOMER_SHEET_CONFIRM_BUTTON_TEST_TAG
import com.stripe.android.customersheet.ui.CUSTOMER_SHEET_SAVE_BUTTON_TEST_TAG
import com.stripe.android.model.PaymentMethodCode
import com.stripe.android.paymentelement.embedded.form.EMBEDDED_FORM_ACTIVITY_PRIMARY_BUTTON
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.example.BuildConfig
import com.stripe.android.paymentsheet.example.playground.PaymentSheetPlaygroundActivity
import com.stripe.android.paymentsheet.example.playground.PlaygroundState
import com.stripe.android.paymentsheet.example.playground.SUCCESS_RESULT
import com.stripe.android.paymentsheet.example.playground.activity.FawryActivity
import com.stripe.android.paymentsheet.example.playground.settings.CheckoutMode
import com.stripe.android.paymentsheet.example.playground.settings.CheckoutModeSettingsDefinition
import com.stripe.android.paymentsheet.example.playground.settings.CollectAddressSettingsDefinition
import com.stripe.android.paymentsheet.example.playground.settings.Country
import com.stripe.android.paymentsheet.example.playground.settings.CountrySettingsDefinition
import com.stripe.android.paymentsheet.example.playground.settings.CustomerSettingsDefinition
import com.stripe.android.paymentsheet.example.playground.settings.CustomerType
import com.stripe.android.paymentsheet.example.playground.settings.Layout
import com.stripe.android.paymentsheet.example.playground.settings.LayoutSettingsDefinition
import com.stripe.android.paymentsheet.example.playground.settings.PlaygroundConfigurationData
import com.stripe.android.paymentsheet.example.playground.settings.RequireCvcRecollectionDefinition
import com.stripe.android.paymentsheet.ui.PAYMENT_SHEET_ERROR_TEXT_TEST_TAG
import com.stripe.android.paymentsheet.ui.SAVED_PAYMENT_METHOD_CARD_TEST_TAG
import com.stripe.android.paymentsheet.verticalmode.TEST_TAG_NEW_PAYMENT_METHOD_ROW_BUTTON
import com.stripe.android.paymentsheet.verticalmode.TEST_TAG_PAYMENT_METHOD_EMBEDDED_LAYOUT
import com.stripe.android.paymentsheet.verticalmode.TEST_TAG_PAYMENT_METHOD_VERTICAL_LAYOUT
import com.stripe.android.test.core.ui.BrowserUI
import com.stripe.android.test.core.ui.ComposeButton
import com.stripe.android.test.core.ui.Selectors
import com.stripe.android.test.core.ui.UiAutomatorText
import kotlinx.coroutines.launch
import org.junit.Assert.fail
import org.junit.Assume
import org.junit.Assume.assumeFalse
import org.junit.Assume.assumeTrue
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/**
 * This drives the end to end payment sheet flow for any set of
 * [TestParameters].  It handles any authorization that needs to happen
 * It drives the test from the test playground so that a payment
 * intent does not need to be created.
 *
 * This does not yet work when the locale is not english.
 * It works for all screen sizes
 * It does not test every possible drop down parameter
 */
internal class PlaygroundTestDriver(
    private val device: UiDevice,
    private val composeTestRule: ComposeTestRule,
) : ScreenshotTest {
    @Volatile
    private var resultCountDownLatch: CountDownLatch? = null

    @Volatile
    private var resultValue: String? = null
    private lateinit var testParameters: TestParameters
    private lateinit var selectors: Selectors

    @Volatile
    private var currentActivity: Activity? = null
    private var application: Application? = null

    @Volatile
    private var playgroundState: PlaygroundState? = null

    private val activityLifecycleCallbacks = object : Application.ActivityLifecycleCallbacks {
        override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {}
        override fun onActivityStarted(activity: Activity) {}
        override fun onActivityPaused(activity: Activity) {}
        override fun onActivityStopped(activity: Activity) {}
        override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}
        override fun onActivityDestroyed(activity: Activity) {}
        override fun onActivityResumed(activity: Activity) {
            currentActivity = activity
        }
    }

    fun testLinkCustom(
        testParameters: TestParameters,
        values: FieldPopulator.Values = FieldPopulator.Values(),
        populateCustomLpmFields: FieldPopulator.() -> Unit = {},
        verifyCustomLpmFields: FieldPopulator.() -> Unit = {},
    ) {
        setup(testParameters)
        launchCustom()

        composeTestRule.waitForIdle()

        composeTestRule.waitUntil(timeoutMillis = 5000L) {
            selectors.addPaymentMethodButton.isDisplayed()
        }

        addPaymentMethodNode().apply {
            assertExists()
            performClick()
        }

        val fieldPopulator = FieldPopulator(
            selectors,
            testParameters,
            populateCustomLpmFields,
            verifyCustomLpmFields,
            values,
        )
        fieldPopulator.populateFields()

        composeTestRule.onNodeWithText("Save my info for secure 1-click checkout").apply {
            assertExists()
            performClick()
        }

        composeTestRule.onNodeWithText("Email").apply {
            assertExists()
            performTextInput("email@email.com")
        }

        closeSoftKeyboard()

        runCatching {
            // We need to wait for the built in debounce time for filling in the link email.
            composeTestRule.waitUntil(timeoutMillis = 1100L) {
                false
            }
        }

        composeTestRule.waitUntil(timeoutMillis = 5000L) {
            selectors.continueButton.checkEnabled()
        }

        composeTestRule.waitForIdle()

        selectors.continueButton.click()

        composeTestRule.waitUntil(timeoutMillis = 5000L) {
            composeTestRule.onAllNodesWithTag("OTP-0").fetchSemanticsNodes().isNotEmpty()
        }

        composeTestRule.onNodeWithTag("OTP-0").performTextInput("123456")

        Espresso.onIdle()
        composeTestRule.waitForIdle()

        waitForPlaygroundActivity()

        selectors.multiStepSelect.click()

        waitForNotPlaygroundActivity()

        composeTestRule.waitUntil(timeoutMillis = 5000L) {
            composeTestRule.onAllNodesWithTag("SignedInBox").fetchSemanticsNodes().isNotEmpty()
        }

        Espresso.onIdle()
        composeTestRule.waitForIdle()

        fieldPopulator.verifyFields()

        teardown()
    }

    fun confirmCustom(
        testParameters: TestParameters,
        values: FieldPopulator.Values = FieldPopulator.Values(),
        populateCustomLpmFields: FieldPopulator.() -> Unit = {},
        verifyCustomLpmFields: FieldPopulator.() -> Unit = {},
    ): PlaygroundState? {
        setup(
            testParameters.copyPlaygroundSettings { settings ->
                settings.updateConfigurationData { configurationData ->
                    configurationData.copy(
                        integrationType = PlaygroundConfigurationData.IntegrationType.FlowController
                    )
                }
            }
        )
        launchCustom()

        if (isSelectPaymentMethodScreen()) {
            // When Link is enabled we get the select screen, but we want to go to the add screen
            // and click the payment method.
            addPaymentMethodNode().performClick()
        }
        clickPaymentSelection()

        val fieldPopulator = FieldPopulator(
            selectors,
            testParameters,
            populateCustomLpmFields,
            verifyCustomLpmFields,
            values,
        )
        fieldPopulator.populateFields()

        val result = playgroundState

        Espresso.onIdle()
        composeTestRule.waitForIdle()

        pressContinue()

        pressMultiStepSelect()

        Espresso.onIdle()
        composeTestRule.waitForIdle()

        fieldPopulator.verifyFields()

        teardown()

        return result
    }

    fun confirmCustomAndBuy(
        testParameters: TestParameters,
        values: FieldPopulator.Values = FieldPopulator.Values(),
        populateCustomLpmFields: FieldPopulator.() -> Unit = {},
        customerId: String? = null
    ): PlaygroundState? {
        setup(
            testParameters.copyPlaygroundSettings { settings ->
                settings.updateConfigurationData { configurationData ->
                    configurationData.copy(
                        integrationType = PlaygroundConfigurationData.IntegrationType.FlowController
                    )
                }

                customerId?.let { id ->
                    settings[CustomerSettingsDefinition] = CustomerType.Existing(id)
                }
            }
        )
        launchCustom()

        if (isSelectPaymentMethodScreen()) {
            // When Link is enabled we get the select screen, but we want to go to the add screen
            // and click the payment method.
            addPaymentMethodNode().performClick()
        }
        clickPaymentSelection()

        val fieldPopulator = FieldPopulator(
            selectors,
            testParameters,
            populateCustomLpmFields,
            verifyCustomLpmFields = {},
            values,
        )
        fieldPopulator.populateFields()

        val result = playgroundState

        Espresso.onIdle()
        composeTestRule.waitForIdle()

        pressContinue()

        resultCountDownLatch = testParameters.countDownLatch()
        selectors.playgroundBuyButton.click()

        doAuthorization()

        teardown()

        return result
    }

    fun confirmCustomWithDefaultSavedPaymentMethod(
        customerId: String?,
        testParameters: TestParameters,
        beforeBuyAction: (Selectors) -> Unit = {},
        afterBuyAction: (Selectors) -> Unit = {},
    ) {
        if (customerId == null) {
            fail("No customer id")
            return
        }

        setup(
            testParameters.copyPlaygroundSettings { settings ->
                settings.updateConfigurationData { configurationData ->
                    configurationData.copy(
                        integrationType = PlaygroundConfigurationData.IntegrationType.FlowController
                    )
                }

                settings[CustomerSettingsDefinition] = CustomerType.Existing(customerId)
            }
        )
        launchCustom(clickMultiStep = false)

        beforeBuyAction(selectors)

        resultCountDownLatch = testParameters.countDownLatch()
        selectors.playgroundBuyButton.click()

        afterBuyAction(selectors)

        doAuthorization()

        teardown()
    }

    fun savePaymentMethodInCustomerSheet(
        testParameters: TestParameters,
        values: FieldPopulator.Values = FieldPopulator.Values(),
        populateCustomLpmFields: FieldPopulator.() -> Unit = {},
    ): PlaygroundState? {
        setup(
            testParameters.copyPlaygroundSettings { settings ->
                settings.updateConfigurationData { configurationData ->
                    configurationData.copy(
                        integrationType = PlaygroundConfigurationData.IntegrationType.CustomerSheet
                    )
                }
            }
        )

        launchCustomerSheet()

        clickPaymentSelection()

        val fieldPopulator = FieldPopulator(
            selectors,
            testParameters,
            populateCustomLpmFields,
            {},
            values,
        )
        fieldPopulator.populateFields()

        val result = playgroundState

        pressCustomerSheetSave()

        doAuthorization()

        pressCustomerSheetConfirm()

        teardown()

        return result
    }

    fun saveUsBankAccountInCustomerSheet(
        testParameters: TestParameters,
        values: FieldPopulator.Values = FieldPopulator.Values(),
        populateCustomLpmFields: FieldPopulator.() -> Unit = {},
    ): PlaygroundState? {
        setup(
            testParameters.copyPlaygroundSettings { settings ->
                settings.updateConfigurationData { configurationData ->
                    configurationData.copy(
                        integrationType = PlaygroundConfigurationData.IntegrationType.CustomerSheet
                    )
                }
            }
        )

        launchCustomerSheet()

        clickPaymentSelection()

        val fieldPopulator = FieldPopulator(
            selectors,
            testParameters,
            populateCustomLpmFields,
            {},
            values,
        )
        fieldPopulator.populateFields()

        // Verify device requirements are met prior to attempting confirmation.
        verifyDeviceSupportsTestAuthorization(
            testParameters.authorizationAction,
            testParameters.useBrowser
        )

        val result = playgroundState

        pressCustomerSheetSave()

        executeUsBankAccountFlow()

        waitForCustomerSheetSaveButton()

        pressCustomerSheetSave()

        waitForCustomerSheetConfirmButton()

        pressCustomerSheetConfirm()

        teardown()

        return result
    }

    fun confirmWithGooglePay(
        country: Country,
        checkoutMode: CheckoutMode = CheckoutMode.PAYMENT,
    ) {
        setup(
            TestParameters.create(
                paymentMethodCode = "card",
            ) { settings ->
                settings[CountrySettingsDefinition] = country
                settings[CheckoutModeSettingsDefinition] = checkoutMode
            }
        )

        launchComplete()

        Espresso.onIdle()
        composeTestRule.waitForIdle()

        selectors.googlePayButton.waitForEnabled()
        selectors.googlePayButton.click()

        composeTestRule.waitForIdle()

        selectors.googlePaySheet.waitFor()
        selectors.googlePayContinueButton.click()

        composeTestRule.waitForIdle()

        // Skips the full screen payment animation in `PaymentSheet`
        while (currentActivity !is PaymentSheetPlaygroundActivity) {
            composeTestRule.mainClock.advanceTimeByFrame()
        }

        Espresso.onIdle()
        composeTestRule.waitForIdle()

        teardown()
    }

    private fun pressMultiStepSelect() {
        selectors.multiStepSelect.click()
        waitForNotPlaygroundActivity()
    }

    private fun pressContinue(waitForPlayground: Boolean = true) {
        selectors.continueButton.click()
        if (waitForPlayground) {
            waitForPlaygroundActivity()
        }
    }

    private fun pressCustomerSheetSave() {
        Espresso.onIdle()
        composeTestRule.waitForIdle()

        selectors.customerSheetSaveButton.click()

        Espresso.onIdle()
        composeTestRule.waitForIdle()
    }

    private fun pressCustomerSheetConfirm() {
        Espresso.onIdle()
        composeTestRule.waitForIdle()

        selectors.customerSheetConfirmButton.click()

        Espresso.onIdle()
        composeTestRule.waitForIdle()
    }

    /**
     * This will open the payment sheet complete flow from the playground with a new or
     * guest user and complete the confirmation including any browser interactions.
     *
     * A test calling this takes about 25s on average to run.
     */
    fun confirmNewOrGuestComplete(
        testParameters: TestParameters,
        values: FieldPopulator.Values? = FieldPopulator.Values(),
        afterAuthorization: (Selectors) -> Unit = {},
        populateCustomLpmFields: FieldPopulator.() -> Unit = {},
    ): PlaygroundState? {
        setup(testParameters)
        launchComplete()

        if (testParameters.playgroundSettingsSnapshot[LayoutSettingsDefinition] != Layout.HORIZONTAL) {
            selectLpmInVerticalMode(testParameters.paymentMethodCode)
        } else {
            clickPaymentSelection()
        }

        if (values != null) {
            FieldPopulator(
                selectors,
                testParameters,
                populateCustomLpmFields,
                verifyCustomLpmFields = {},
                values = values,
            ).populateFields()
        }

        // Verify device requirements are met prior to attempting confirmation.  Do this
        // after we have had the chance to capture a screenshot.
        verifyDeviceSupportsTestAuthorization(
            testParameters.authorizationAction,
            testParameters.useBrowser
        )

        val result = playgroundState

        pressBuy()

        doAuthorization()

        afterAuthorization(selectors)

        teardown()

        return result
    }

    fun confirmEmbedded(
        testParameters: TestParameters,
        values: FieldPopulator.Values? = FieldPopulator.Values(),
        afterAuthorization: (Selectors) -> Unit = {},
        populateCustomLpmFields: FieldPopulator.() -> Unit = {},
    ): PlaygroundState? {
        setup(
            testParameters.copyPlaygroundSettings { settings ->
                settings.updateConfigurationData { configurationData ->
                    configurationData.copy(
                        integrationType = PlaygroundConfigurationData.IntegrationType.Embedded
                    )
                }
            }
        )
        launchEmbedded()

        selectLpmInEmbeddedMode(testParameters.paymentMethodCode)

        if (values != null) {
            FieldPopulator(
                selectors,
                testParameters,
                populateCustomLpmFields,
                verifyCustomLpmFields = {},
                values = values,
            ).populateFields()
        }

        // Verify device requirements are met prior to attempting confirmation.  Do this
        // after we have had the chance to capture a screenshot.
        verifyDeviceSupportsTestAuthorization(
            testParameters.authorizationAction,
            testParameters.useBrowser
        )

        val result = playgroundState

        if (values != null) {
            selectors.embeddedFormBuyButton.waitForEnabled(requireClickAction = false)
            selectors.embeddedFormBuyButton.click()
        } else {
            selectors.complete.click()
        }

        doAuthorization()

        afterAuthorization(selectors)

        teardown()

        return result
    }

    fun confirmEmbeddedUsBankAccount(
        testParameters: TestParameters,
        values: FieldPopulator.Values = FieldPopulator.Values(),
    ): PlaygroundState? {
        setup(
            testParameters.copyPlaygroundSettings { settings ->
                settings.updateConfigurationData { configurationData ->
                    configurationData.copy(
                        integrationType = PlaygroundConfigurationData.IntegrationType.Embedded
                    )
                }
            }
        )
        launchEmbedded()

        selectLpmInEmbeddedMode(testParameters.paymentMethodCode)

        FieldPopulator(
            selectors,
            testParameters,
            populateCustomLpmFields = {},
            verifyCustomLpmFields = {},
            values = values,
        ).populateFields()

        // Verify device requirements are met prior to attempting confirmation.  Do this
        // after we have had the chance to capture a screenshot.
        verifyDeviceSupportsTestAuthorization(
            testParameters.authorizationAction,
            testParameters.useBrowser
        )

        val result = playgroundState

        selectors.embeddedFormBuyButton.click()

        doUSBankAccountAuthorization(testParameters.authorizationAction)

        selectors.embeddedFormBuyButton.waitForEnabled(requireClickAction = false)
        selectors.embeddedFormBuyButton.click()
        finishAfterAuthorization()

        teardown()

        return result
    }

    fun confirmExistingComplete(
        customerId: String?,
        testParameters: TestParameters,
        values: FieldPopulator.Values = FieldPopulator.Values(),
        beforeBuyAction: (Selectors) -> Unit = {},
        afterBuyAction: (Selectors) -> Unit = {},
        populateCustomLpmFields: FieldPopulator.() -> Unit = {},
    ): PlaygroundState? {
        if (customerId == null) {
            fail("No customer id")
            return playgroundState
        }

        setup(
            testParameters.copyPlaygroundSettings { settings ->
                settings[CustomerSettingsDefinition] = CustomerType.Existing(customerId)
            }
        )
        launchComplete()

        waitForAddPaymentMethodNode()
        addPaymentMethodNode().performClick()

        clickPaymentSelection()

        FieldPopulator(
            selectors,
            testParameters,
            populateCustomLpmFields,
            verifyCustomLpmFields = {},
            values = values,
        ).populateFields()

        // Verify device requirements are met prior to attempting confirmation.  Do this
        // after we have had the chance to capture a screenshot.
        verifyDeviceSupportsTestAuthorization(
            testParameters.authorizationAction,
            testParameters.useBrowser
        )

        val result = playgroundState

        beforeBuyAction(selectors)

        pressBuy()

        doAuthorization()

        afterBuyAction(selectors)

        teardown()

        return result
    }

    /**
     * This will open the payment sheet complete flow from the playground with an existing
     * user and complete the confirmation including any browser interactions.
     */
    fun confirmCompleteWithDefaultSavedPaymentMethod(
        customerId: String?,
        testParameters: TestParameters,
        beforeBuyAction: (Selectors) -> Unit = {},
        afterBuyAction: (Selectors) -> Unit = {},
    ): PlaygroundState? {
        if (customerId == null) {
            fail("No customer id")
            return playgroundState
        }

        setup(
            testParameters.copyPlaygroundSettings { settings ->
                settings[CustomerSettingsDefinition] = CustomerType.Existing(customerId)
            }
        )
        launchComplete()

        val result = playgroundState

        beforeBuyAction(selectors)

        pressBuy()

        doAuthorization()

        afterBuyAction(selectors)

        teardown()

        return result
    }

    fun confirmCompleteWithSavePaymentMethodAndCvcRecollection(
        customerId: String?,
        testParameters: TestParameters
    ): PlaygroundState? {
        if (customerId == null) {
            fail("No customer id")
            return playgroundState
        }
        setup(
            testParameters.copyPlaygroundSettings { settings ->
                settings[CustomerSettingsDefinition] = CustomerType.Existing(customerId)
                settings[RequireCvcRecollectionDefinition] = true
            }
        )
        launchComplete()

        selectors.getCardCvc()
            .performScrollTo().performTextInput("123")

        val result = playgroundState

        pressBuy()

        doAuthorization()

        teardown()

        return result
    }

    fun confirmCustomWithSavePaymentMethodAndCvcRecollection(
        customerId: String?,
        testParameters: TestParameters
    ): PlaygroundState? {
        if (customerId == null) {
            fail("No customer id")
            return playgroundState
        }
        setup(
            testParameters.copyPlaygroundSettings { settings ->
                settings[CustomerSettingsDefinition] = CustomerType.Existing(customerId)
                settings[RequireCvcRecollectionDefinition] = true
                settings.updateConfigurationData { configurationData ->
                    configurationData.copy(
                        integrationType = PlaygroundConfigurationData.IntegrationType.FlowController
                    )
                }
            }
        )

        launchCustom(false)

        resultCountDownLatch = testParameters.countDownLatch()
        selectors.playgroundBuyButton.click()

        selectors.getCardCvc().performTextInput("123")

        selectors.getCvcRecollectionScreenConfirm().performClick()

        val result = playgroundState

        doAuthorization()

        teardown()

        return result
    }

    fun confirmExternalPaymentMethodSuccess(
        testParameters: TestParameters,
    ) {
        setup(testParameters)
        launchComplete()

        confirmExternalPaymentMethod(
            selectors.externalPaymentMethodSucceedButton,
        )

        waitForPlaygroundActivity()

        assertThat(resultValue).isEqualTo(SUCCESS_RESULT)

        teardown()
    }

    fun confirmExternalPaymentMethodCanceled(
        testParameters: TestParameters,
    ) {
        setup(testParameters)
        launchComplete()

        confirmExternalPaymentMethod(
            selectors.externalPaymentMethodCancelButton,
        )

        isSelectPaymentMethodScreen()
        selectors.buyButton.isEnabled()

        teardown()
    }

    fun confirmExternalPaymentMethodFailed(
        testParameters: TestParameters,
    ) {
        setup(testParameters)
        launchComplete()

        confirmExternalPaymentMethod(
            selectors.externalPaymentMethodFailButton,
        )

        composeTestRule.onNode(hasTestTag(PAYMENT_SHEET_ERROR_TEXT_TEST_TAG))
            .assertIsDisplayed()
            .assertTextEquals(FawryActivity.FAILED_DISPLAY_MESSAGE)

        teardown()
    }

    private fun confirmExternalPaymentMethod(
        button: ComposeButton,
    ) {
        clickPaymentSelection()

        pressBuy()

        button.waitForEnabled()
        button.click()
    }

    fun confirmUSBankAccount(
        testParameters: TestParameters,
        afterAuthorization: (Selectors, FieldPopulator) -> Unit = { _, _ -> },
    ): PlaygroundState? {
        return confirmBankAccount(
            testParameters = testParameters,
            executeFlow = { doUSBankAccountAuthorization(testParameters.authorizationAction) },
            afterCollectingBankInfo = afterAuthorization,
            confirmIntent = true,
        )
    }

    fun confirmCustomUSBankAccount(
        testParameters: TestParameters,
        afterAuthorization: (Selectors) -> Unit = {},
    ) {
        confirmBankAccountInCustomFlow(
            testParameters = testParameters,
            executeFlow = { doUSBankAccountAuthorization(testParameters.authorizationAction) },
            afterCollectingBankInfo = afterAuthorization,
        )
    }

    @OptIn(ExperimentalTestApi::class)
    fun confirmWithBankAccountInLink(
        testParameters: TestParameters,
    ) {
        setup(testParameters)

        launchComplete()

        Espresso.onIdle()
        composeTestRule.waitForIdle()

        // Expect the OTP dialog
        composeTestRule.waitUntilExactlyOneExists(
            matcher = hasTestTag("OTP-0"),
            timeoutMillis = DEFAULT_UI_TIMEOUT.inWholeMilliseconds,
        )

        composeTestRule
            .onNodeWithTag("OTP-0")
            .performTextInput("000000")

        composeTestRule.waitUntilExactlyOneExists(
            matcher = hasTestTag("collapsed_wallet_row_tag"),
            timeoutMillis = DEFAULT_UI_TIMEOUT.inWholeMilliseconds,
        )

        composeTestRule
            .onNodeWithTag("collapsed_wallet_row_tag")
            .performClick()

        // We might have more than one bank account
        composeTestRule
            .onAllNodesWithText("Test Institution")
            .onFirst()
            .performClick()

        composeTestRule
            .onNodeWithTag("wallet_screen_pay_button")
            .performClick()

        composeTestRule.waitForIdle()

        // Skips the full screen payment animation in `PaymentSheet`
        while (currentActivity !is PaymentSheetPlaygroundActivity) {
            composeTestRule.mainClock.advanceTimeByFrame()
        }

        Espresso.onIdle()
        composeTestRule.waitForIdle()

        teardown()
    }

    fun confirmLinkBankPayment(
        testParameters: TestParameters,
        afterAuthorization: (Selectors, FieldPopulator) -> Unit = { _, _ -> },
    ): PlaygroundState? {
        return confirmBankAccount(
            testParameters = testParameters,
            executeFlow = { doInstantDebitsFlow(testParameters.authorizationAction) },
            afterCollectingBankInfo = afterAuthorization,
            confirmIntent = testParameters.authorizationAction == null,
        )
    }

    fun confirmInstantDebitsInCustomFlow(
        testParameters: TestParameters,
        afterAuthorization: (Selectors) -> Unit = {},
    ) {
        confirmBankAccountInCustomFlow(
            testParameters = testParameters,
            executeFlow = { doInstantDebitsFlow(testParameters.authorizationAction) },
            afterCollectingBankInfo = afterAuthorization,
        )
    }

    private fun confirmBankAccount(
        testParameters: TestParameters,
        executeFlow: () -> Unit,
        afterCollectingBankInfo: (Selectors, FieldPopulator) -> Unit = { _, _ -> },
        confirmIntent: Boolean,
    ): PlaygroundState? {
        setup(testParameters)
        launchComplete()

        clickPaymentSelection()

        val populator = FieldPopulator(
            selectors = selectors,
            testParameters = testParameters,
            populateCustomLpmFields = {},
            verifyCustomLpmFields = {},
            values = FieldPopulator.Values(),
        )

        populator.populateFields()

        // Verify device requirements are met prior to attempting confirmation.  Do this
        // after we have had the chance to capture a screenshot.
        verifyDeviceSupportsTestAuthorization(
            testParameters.authorizationAction,
            testParameters.useBrowser
        )

        val result = playgroundState

        pressBuy()

        executeFlow()

        afterCollectingBankInfo(selectors, populator)

        if (confirmIntent) {
            pressBuy()
            finishAfterAuthorization()
        }

        teardown()

        return result
    }

    private fun confirmBankAccountInCustomFlow(
        testParameters: TestParameters,
        executeFlow: PlaygroundTestDriver.() -> Unit,
        afterCollectingBankInfo: (Selectors) -> Unit = {},
    ) {
        setup(
            testParameters.copyPlaygroundSettings { settings ->
                settings.updateConfigurationData { configurationData ->
                    configurationData.copy(
                        integrationType = PlaygroundConfigurationData.IntegrationType.FlowController
                    )
                }
            }
        )
        launchCustom()

        if (isSelectPaymentMethodScreen()) {
            // When Link is enabled we get the select screen, but we want to go to the add screen
            // and click the payment method.
            addPaymentMethodNode().performClick()
        }
        clickPaymentSelection()

        FieldPopulator(
            selectors = selectors,
            testParameters = testParameters,
            populateCustomLpmFields = {},
            verifyCustomLpmFields = {},
            values = FieldPopulator.Values(),
        ).populateFields()

        // Verify device requirements are met prior to attempting confirmation.  Do this
        // after we have had the chance to capture a screenshot.
        verifyDeviceSupportsTestAuthorization(
            testParameters.authorizationAction,
            testParameters.useBrowser
        )

        pressContinue(waitForPlayground = false)

        this.executeFlow()

        afterCollectingBankInfo(selectors)

        teardown()
    }

    /**
     * This test will open the payment sheet complete flow and take a picture when it has finished
     * opening. The sheet is then closed. We will use the screenshot to compare o a golden value
     * in our repository.
     *
     * A test calling this takes about 20 seconds
     */
    fun screenshotRegression(
        testParameters: TestParameters,
        customOperations: () -> Unit = {}
    ) {
        setup(testParameters)
        launchComplete()

        composeTestRule.waitForIdle()
        device.waitForIdle()

        waitForScreenToLoad(testParameters)
        customOperations()

        currentActivity?.let {
            compareScreenshot(it)
        }

        teardown()
    }

    private fun waitForScreenToLoad(testParameters: TestParameters) {
        when (testParameters.playgroundSettingsSnapshot[CustomerSettingsDefinition]) {
            is CustomerType.GUEST, is CustomerType.NEW -> {
                composeTestRule.waitUntil(timeoutMillis = DEFAULT_UI_TIMEOUT.inWholeMilliseconds) {
                    composeTestRule.onAllNodesWithText("Card number")
                        .fetchSemanticsNodes()
                        .size == 1
                }

                val collectionMode = testParameters.playgroundSettingsSnapshot[CollectAddressSettingsDefinition]

                if (collectionMode != PaymentSheet.BillingDetailsCollectionConfiguration.AddressCollectionMode.Never) {
                    composeTestRule.waitUntil {
                        composeTestRule.onAllNodesWithText("Country or region")
                            .fetchSemanticsNodes()
                            .size == 1
                    }
                }
            }
            is CustomerType.Existing, is CustomerType.RETURNING -> {
                composeTestRule.waitUntil(timeoutMillis = DEFAULT_UI_TIMEOUT.inWholeMilliseconds) {
                    composeTestRule.onAllNodesWithTag("AddCard")
                        .fetchSemanticsNodes()
                        .size == 1
                }
            }
        }
    }

    private fun pressBuy() {
        selectors.buyButton.click()
    }

    internal fun pressSelection() {
        composeTestRule.waitForIdle()

        clickPaymentSelection()
    }

    internal fun scrollToBottom() {
        composeTestRule.waitForIdle()

        selectors.buyButton.scrollTo()
    }

    internal fun pressEdit() {
        composeTestRule.waitUntil(timeoutMillis = DEFAULT_UI_TIMEOUT.inWholeMilliseconds) {
            composeTestRule
                .onAllNodesWithText("EDIT")
                .fetchSemanticsNodes()
                .isNotEmpty()
        }

        composeTestRule
            .onNodeWithText("EDIT")
            .performClick()
    }

    private fun clickPaymentSelection() {
        selectors.formElement.waitFor()
        selectors.paymentSelection.click()

        Espresso.onIdle()
        composeTestRule.waitForIdle()
    }

    private fun selectLpmInVerticalMode(paymentMethodCode: PaymentMethodCode) {
        composeTestRule.waitUntil(DEFAULT_UI_TIMEOUT.inWholeMilliseconds) {
            composeTestRule
                .onAllNodes(hasTestTag(TEST_TAG_PAYMENT_METHOD_VERTICAL_LAYOUT))
                .fetchSemanticsNodes()
                .isNotEmpty()
        }

        composeTestRule.onNode(hasTestTag("${TEST_TAG_NEW_PAYMENT_METHOD_ROW_BUTTON}_$paymentMethodCode"))
            .performScrollTo()
            .performClick()

        Espresso.onIdle()
        composeTestRule.waitForIdle()
    }

    private fun selectLpmInEmbeddedMode(paymentMethodCode: PaymentMethodCode) {
        composeTestRule.waitUntil(DEFAULT_UI_TIMEOUT.inWholeMilliseconds) {
            composeTestRule
                .onAllNodes(hasTestTag(TEST_TAG_PAYMENT_METHOD_EMBEDDED_LAYOUT))
                .fetchSemanticsNodes()
                .isNotEmpty()
        }

        composeTestRule.onNode(hasTestTag("${TEST_TAG_NEW_PAYMENT_METHOD_ROW_BUTTON}_$paymentMethodCode"))
            .performScrollTo()
            .performClick()

        Espresso.onIdle()
        composeTestRule.waitForIdle()
    }

    private fun waitUntilPrimaryButtonIsCompleted() {
        composeTestRule.waitUntil(DEFAULT_UI_TIMEOUT.inWholeMilliseconds) {
            composeTestRule.onAllNodesWithTag(EMBEDDED_FORM_ACTIVITY_PRIMARY_BUTTON)
                .fetchSemanticsNodes(atLeastOneRootRequired = false)
                .isEmpty()
        }

        composeTestRule.waitForIdle()
        Espresso.onIdle()
    }

    /**
     * Here we wait for an activity different from the playground to be in view.  We
     * don't specifically look for PaymentSheetActivity or PaymentOptionsActivity because
     * that would require exposing the activities publicly.
     */
    private fun waitForNotPlaygroundActivity() {
        while (currentActivity is PaymentSheetPlaygroundActivity) {
            TimeUnit.MILLISECONDS.sleep(250)
        }
        Espresso.onIdle()
        composeTestRule.waitForIdle()
    }

    /**
     * Here we wait for the Playground to come back into view.
     */
    private fun waitForPlaygroundActivity() {
        while (currentActivity !is PaymentSheetPlaygroundActivity) {
            TimeUnit.MILLISECONDS.sleep(250)
        }
        Espresso.onIdle()
        composeTestRule.waitForIdle()
    }

    /**
     * Here we wait for PollingActivity to first come into view then wait for it to go away by checking if the Approve payment text is there
     */
    private fun waitForPollingToFinish(timeout: Duration = 60.seconds) {
        val className =
            "com.stripe.android.paymentsheet.paymentdatacollection.polling.PollingActivity"
        while (currentActivity?.componentName?.className != className) {
            Thread.sleep(10)
        }

        composeTestRule.waitUntil(timeoutMillis = timeout.inWholeMilliseconds) {
            try {
                composeTestRule
                    .onAllNodesWithText("Approve payment")
                    .fetchSemanticsNodes()
                    .isEmpty()
            } catch (e: IllegalStateException) {
                // PollingActivity was closed
                true
            }
        }
    }

    private fun verifyDeviceSupportsTestAuthorization(
        authorizeAction: AuthorizeAction?,
        requestedBrowser: Browser?
    ) {
        if (authorizeAction?.requiresBrowser == true) {
            requestedBrowser?.let {
                val browserUI = BrowserUI.convert(it)
                Assume.assumeTrue(getBrowser(browserUI) == browserUI)
            } ?: Assume.assumeTrue(selectors.getInstalledBrowsers().isNotEmpty())
        }
        if (authorizeAction == AuthorizeAction.DisplayQrCode) {
            // Browserstack tests fail on pixel 2 API 26.
            assumeFalse("walleye + 26" == "${Build.DEVICE} + ${Build.VERSION.SDK_INT}")
        }
    }

    private fun getBrowser(requestedBrowser: BrowserUI?): BrowserUI {
        val installedBrowsers = selectors.getInstalledBrowsers()

        return requestedBrowser?.let {
            // Assume true will mark the test as skipped if it can't be executed
            Assume.assumeTrue(installedBrowsers.contains(it))
            it
        } ?: installedBrowsers.first()
    }

    private fun monitorCurrentActivity(application: Application) {
        this.application = application
        application.registerActivityLifecycleCallbacks(activityLifecycleCallbacks)
    }

    private fun launchComplete() {
        selectors.reload.click()
        selectors.complete.waitForEnabled()
        selectors.complete.click()

        // PaymentSheetActivity is now on screen
        waitForNotPlaygroundActivity()
    }

    private fun launchCustom(clickMultiStep: Boolean = true) {
        selectors.reload.click()
        Espresso.onIdle()
        selectors.composeTestRule.waitForIdle()

        selectors.multiStepSelect.waitForEnabled()
        if (clickMultiStep) {
            selectors.multiStepSelect.click()

            // PaymentOptionsActivity is now on screen
            waitForNotPlaygroundActivity()
        }
    }

    private fun launchEmbedded() {
        selectors.reload.click()
        selectors.complete.waitForEnabled()
        selectors.complete.click()

        // EmbeddedPlaygroundActivity is now on screen.
        waitForNotPlaygroundActivity()
    }

    private fun launchCustomerSheet() {
        selectors.reload.click()
        Espresso.onIdle()
        selectors.composeTestRule.waitForIdle()

        selectors.multiStepSelect.waitForEnabled()
        selectors.multiStepSelect.click()

        waitForNotPlaygroundActivity()
    }

    private fun doAuthorization() {
        selectors.apply {
            if (testParameters.authorizationAction != null) {
                if (testParameters.authorizationAction?.requiresBrowser == true) {
                    // If a specific browser is requested we will use it, otherwise, we will
                    // select the first browser found
                    val selectedBrowser = getBrowser(BrowserUI.convert(testParameters.useBrowser))

                    // If there are multiple browser there is a browser selector window
                    selectBrowserPrompt.wait(4000)
                    if (selectBrowserPrompt.exists()) {
                        browserIconAtPrompt(selectedBrowser).click()
                    }

                    assertThat(browserWindow(selectedBrowser)?.exists()).isTrue()

                    blockUntilAuthorizationPageLoaded(isSetup = testParameters.isSetupMode)
                }

                if (authorizeAction != null) {
                    if (authorizeAction.exists()) {
                        authorizeAction.click()
                    } else if (!authorizeAction.exists()) {
                        // Buttons aren't showing the same way each time in the web page.
                        object : UiAutomatorText(
                            label = requireNotNull(testParameters.authorizationAction)
                                .text(testParameters.isSetupMode),
                            className = "android.widget.TextView",
                            device = device
                        ) {
                            override fun click() {
                                if (testParameters.paymentMethodCode == "wechat_pay") {
                                    wait(5000)
                                }
                                super.click()
                            }
                        }.click()
                        Log.e("Stripe", "Fail authorization was a text view not a button this time")
                    }
                }

                when (val authAction = testParameters.authorizationAction) {
                    is AuthorizeAction.DisplayQrCode -> {
                        if (!testParameters.isSetupMode) {
                            closeButton.wait(DEFAULT_UI_TIMEOUT.inWholeMilliseconds)
                            onView(withText("CLOSE")).perform(click())
                        }
                    }

                    is AuthorizeAction.Authorize3ds2 -> {
                        closeButton.wait(DEFAULT_UI_TIMEOUT.inWholeMilliseconds)

                        val completeButton = UiAutomatorText("COMPLETE", device = device)

                        completeButton.wait(DEFAULT_UI_TIMEOUT.inWholeMilliseconds)
                        completeButton.click()
                    }

                    is AuthorizeAction.Test3DS2.HSBCHTML -> {
                        val otpButton = UiAutomatorText("OTP", labelMatchesExactly = true, device = device)

                        otpButton.wait(DEFAULT_UI_TIMEOUT.inWholeMilliseconds)
                        otpButton.click()

                        val submitButton = UiAutomatorText("Submit", labelMatchesExactly = true, device = device)
                        submitButton.wait(DEFAULT_UI_TIMEOUT.inWholeMilliseconds)
                        submitButton.click()

                        val enterOTPField = UiAutomatorText("", labelMatchesExactly = true, device = device)
                        enterOTPField.wait(DEFAULT_UI_TIMEOUT.inWholeMilliseconds)
                        enterOTPField.click()
                        enterOTPField.setText("555555")

                        val otpSubmit = UiAutomatorText("Submit", labelMatchesExactly = true, device = device)
                        otpSubmit.wait(DEFAULT_UI_TIMEOUT.inWholeMilliseconds)
                        otpSubmit.click()
                    }

                    is AuthorizeAction.Test3DS2.SingleSelect -> {
                        val completeAuthentication = UiAutomatorText("Complete Authentication", labelMatchesExactly = true, device = device)
                        completeAuthentication.wait(DEFAULT_UI_TIMEOUT.inWholeMilliseconds)
                        completeAuthentication.click()

                        val submitButton = UiAutomatorText("Submit", labelMatchesExactly = true, device = device)
                        submitButton.wait(DEFAULT_UI_TIMEOUT.inWholeMilliseconds)
                        submitButton.click()
                    }

                    is AuthorizeAction.Test3DS2.MultiSelect -> {
                        UiSelector().textContains("Complete Authentication")
                        val completeAuthentication = UiAutomatorText("Complete Authentication", labelMatchesExactly = true, device = device)
                        completeAuthentication.wait(DEFAULT_UI_TIMEOUT.inWholeMilliseconds)
                        device.findObject(UiSelector().textContains("Complete Authentication").index(0)).click()
                        device.findObject(UiSelector().textContains("Complete Authentication").index(1)).click()
                        device.findObject(UiSelector().textContains("Complete Authentication").index(2)).click()

                        val submitButton = UiAutomatorText("Submit", labelMatchesExactly = true, device = device)
                        submitButton.wait(DEFAULT_UI_TIMEOUT.inWholeMilliseconds)
                        submitButton.click()
                    }

                    is AuthorizeAction.Test3DS2.OOB -> {
                        val completeAuthentication = UiAutomatorText("Complete Authentication", labelMatchesExactly = true, device = device)
                        completeAuthentication.wait(DEFAULT_UI_TIMEOUT.inWholeMilliseconds)
                        completeAuthentication.click()
                    }

                    is AuthorizeAction.Test3DS2.OTP -> {
                        val explanationText = UiAutomatorText("For this test", labelMatchesExactly = true, device = device)
                        explanationText.wait(DEFAULT_UI_TIMEOUT.inWholeMilliseconds)

                        val enterOTPField = UiAutomatorText("Enter your code below:", labelMatchesExactly = true, className = "android.widget.EditText", device = device)
                        enterOTPField.wait(DEFAULT_UI_TIMEOUT.inWholeMilliseconds)
                        enterOTPField.click()
                        enterOTPField.setText("424242")

                        val submitButton = UiAutomatorText("Submit", labelMatchesExactly = true, device = device)
                        submitButton.wait(DEFAULT_UI_TIMEOUT.inWholeMilliseconds)
                        submitButton.click()
                    }

                    is AuthorizeAction.AuthorizePayment -> {}
                    is AuthorizeAction.PollingSucceedsAfterDelay -> {
                        waitForPollingToFinish()
                    }

                    is AuthorizeAction.Cancel -> {
                        buyButton.apply {
                            waitProcessingComplete()
                            isEnabled()
                            isDisplayed()
                        }
                    }

                    is AuthorizeAction.Fail -> {
                        buyButton.apply {
                            waitProcessingComplete()
                            isEnabled()
                            isDisplayed()
                        }

                        // The text comes after the buy button animation is complete
                        composeTestRule.waitUntil(timeoutMillis = DEFAULT_UI_TIMEOUT.inWholeMilliseconds) {
                            runCatching {
                                composeTestRule
                                    .onNodeWithText(authAction.expectedError)
                                    .assertIsDisplayed()
                            }.isSuccess
                        }
                    }
                    is AuthorizeAction.Bacs.Confirm -> {}
                    is AuthorizeAction.Bacs.ModifyDetails -> {
                        buyButton.apply {
                            scrollTo()
                            waitProcessingComplete()
                            isEnabled()
                            isDisplayed()
                        }
                    }
                    null -> {}
                }
            } else {
                // Make sure there is no prompt and no browser window open
                assertThat(selectBrowserPrompt.exists()).isFalse()
                BrowserUI.values().forEach {
                    assertThat(Selectors.browserWindow(device, it)?.exists() == true).isFalse()
                }
            }
        }

        finishAfterAuthorization()
    }

    private fun finishAfterAuthorization() {
        val authAction = testParameters.authorizationAction
        val isDone = authAction == null || authAction.isConsideredDone

        if (isDone) {
            playgroundState?.integrationType?.let { integrationType ->
                if (integrationType.isPaymentFlow()) {
                    if (integrationType == PlaygroundConfigurationData.IntegrationType.Embedded) {
                        waitUntilPrimaryButtonIsCompleted()
                    }

                    waitForPlaygroundActivity()
                    resultCountDownLatch?.let {
                        assertThat(it.await(5, TimeUnit.SECONDS)).isTrue()
                    }
                    assertThat(resultValue).isEqualTo(SUCCESS_RESULT)
                } else if (integrationType.isCustomerFlow()) {
                    waitForCustomerSheetConfirmButton()
                }
            }
        }
    }

    private fun doInstantDebitsFlow(authAction: AuthorizeAction?) {
        if (authAction == AuthorizeAction.Cancel) {
            cancelInstantDebitsFlowOnLaunch()
        } else {
            executeEntireInstantDebitsFlow()
        }
    }

    private fun cancelInstantDebitsFlowOnLaunch() {
        while (currentActivity?.javaClass?.name != FINANCIAL_CONNECTIONS_ACTIVITY) {
            TimeUnit.MILLISECONDS.sleep(250)
        }

        Espresso.onIdle()
        composeTestRule.waitForIdle()

        Espresso.pressBack()
    }

    private fun executeUsBankAccountFlow() {
        while (currentActivity?.javaClass?.name != FINANCIAL_CONNECTIONS_ACTIVITY) {
            TimeUnit.MILLISECONDS.sleep(250)
        }

        composeTestRule.waitUntil(timeoutMillis = DEFAULT_UI_TIMEOUT.inWholeMilliseconds) {
            composeTestRule
                .onAllNodesWithText("Agree and continue")
                .fetchSemanticsNodes(atLeastOneRootRequired = false)
                .size == 1
        }

        clickButton("Agree and continue")
        clickButton("Test Institution")

        // Verifies bank in web view so Compose hierarchy can detach. Button should be available
        // after web view verification.
        clickButton("Connect account", composeCanDetach = true)

        clickButton("Not now")
        clickButtonWithTag("done_button")
    }

    private fun executeEntireInstantDebitsFlow() = with(device) {
        while (currentActivity?.javaClass?.name != FINANCIAL_CONNECTIONS_ACTIVITY) {
            TimeUnit.MILLISECONDS.sleep(250)
        }

        composeTestRule.waitUntil(timeoutMillis = DEFAULT_UI_TIMEOUT.inWholeMilliseconds) {
            composeTestRule
                .onAllNodesWithText("Agree and continue")
                .fetchSemanticsNodes(atLeastOneRootRequired = false)
                .size == 1
        }

        clickButton("Agree and continue")
        clickButtonWithTag("existing_email-button")
        clickButton("Use test code")
        clickButton("Success")
        clickButton("Connect account")
        clickButtonWithTag("done_button")
    }

    private fun doUSBankAccountAuthorization(authAction: AuthorizeAction?) {
        if (authAction == AuthorizeAction.Cancel) {
            cancelAchFlowOnLaunch()
        } else {
            executeUsBankAccountFlow()
        }
    }

    private fun cancelAchFlowOnLaunch() {
        while (currentActivity?.javaClass?.name != FINANCIAL_CONNECTIONS_ACTIVITY) {
            TimeUnit.MILLISECONDS.sleep(250)
        }

        composeTestRule.waitUntil(timeoutMillis = DEFAULT_UI_TIMEOUT.inWholeMilliseconds) {
            composeTestRule
                .onAllNodesWithText("Agree and continue")
                .fetchSemanticsNodes(atLeastOneRootRequired = false)
                .size == 1
        }

        if (testParameters.authorizationAction == AuthorizeAction.Cancel) {
            selectors.authorizeAction?.click()
        }
    }

    private fun clickButton(text: String, composeCanDetach: Boolean = false) {
        composeTestRule.waitUntil(DEFAULT_UI_TIMEOUT.inWholeMilliseconds) {
            composeTestRule
                .onAllNodesWithText(text)
                .fetchSemanticsNodes(atLeastOneRootRequired = !composeCanDetach)
                .isNotEmpty()
        }

        composeTestRule.onNodeWithText(text).performClick()
    }

    private fun clickButtonWithTag(tag: String, composeCanDetach: Boolean = false) {
        composeTestRule.waitUntil(DEFAULT_UI_TIMEOUT.inWholeMilliseconds) {
            composeTestRule
                .onAllNodesWithTag(tag)
                .fetchSemanticsNodes(atLeastOneRootRequired = !composeCanDetach)
                .isNotEmpty()
        }

        composeTestRule.onNodeWithTag(tag).performClick()
    }

    internal fun setup(testParameters: TestParameters) {
        if (BuildConfig.IS_NIGHTLY_BUILD) {
            assumeTrue(testParameters.executeInNightlyRun)
        }

        if (Build.VERSION.SDK_INT <= 28) {
            val unsupportedAuthorizeActions = setOf(AuthorizeAction.Authorize3ds2, AuthorizeAction.DisplayQrCode)
            assumeFalse(unsupportedAuthorizeActions.contains(testParameters.authorizationAction))
        }

        this.testParameters = testParameters
        this.selectors = Selectors(device, composeTestRule, testParameters)

        val launchPlayground = CountDownLatch(1)

        val intent = PaymentSheetPlaygroundActivity.createTestIntent(
            settingsJson = testParameters.playgroundSettingsSnapshot.asJsonString()
        )

        val scenario = ActivityScenario.launch<PaymentSheetPlaygroundActivity>(intent)
        scenario.onActivity { activity ->
            if (testParameters.resetCustomer) {
                PaymentSheet.resetCustomer(activity.applicationContext)
            }

            monitorCurrentActivity(activity.application)

            IdlingPolicies.setIdlingResourceTimeout(45, TimeUnit.SECONDS)
            IdlingPolicies.setMasterPolicyTimeout(45, TimeUnit.SECONDS)

            // Observe the result of the PaymentSheet completion
            activity.lifecycleScope.launch {
                activity.viewModel.status.collect {
                    resultValue = it?.message
                    if (it?.message != null) {
                        resultCountDownLatch?.countDown()
                    }
                }
            }

            activity.lifecycleScope.launch {
                activity.viewModel.state.collect { playgroundState ->
                    this@PlaygroundTestDriver.playgroundState = playgroundState
                }
            }

            launchPlayground.countDown()
        }

        launchPlayground.await(5, TimeUnit.SECONDS)
    }

    private fun teardown() {
        application?.unregisterActivityLifecycleCallbacks(activityLifecycleCallbacks)
        playgroundState = null
        currentActivity = null
        resultValue = null
        resultCountDownLatch = null
    }

    private fun isSelectPaymentMethodScreen(): Boolean {
        return runCatching {
            composeTestRule.onNodeWithText("Select your payment method").assertIsDisplayed()
        }.isSuccess
    }

    private fun addPaymentMethodNode(): SemanticsNodeInteraction {
        waitForAddPaymentMethodNode()
        return composeTestRule.onNodeWithTag(ADD_PAYMENT_METHOD_NODE_TAG)
    }

    @OptIn(ExperimentalTestApi::class)
    private fun waitForAddPaymentMethodNode() {
        composeTestRule.waitUntilAtLeastOneExists(hasTestTag(ADD_PAYMENT_METHOD_NODE_TAG), 5000L)
    }

    @OptIn(ExperimentalTestApi::class)
    private fun waitForCustomerSheetSaveButton() {
        composeTestRule.waitUntilAtLeastOneExists(
            hasTestTag(CUSTOMER_SHEET_SAVE_BUTTON_TEST_TAG),
            DEFAULT_UI_TIMEOUT.inWholeMilliseconds
        )
    }

    private fun waitForCustomerSheetConfirmButton() {
        composeTestRule.waitUntil(DEFAULT_UI_TIMEOUT.inWholeMilliseconds) {
            composeTestRule
                .onAllNodesWithTag(CUSTOMER_SHEET_CONFIRM_BUTTON_TEST_TAG)
                .fetchSemanticsNodes(atLeastOneRootRequired = false)
                .isNotEmpty()
        }
    }

    private companion object {
        const val ADD_PAYMENT_METHOD_NODE_TAG = "${SAVED_PAYMENT_METHOD_CARD_TEST_TAG}_+ Add"
        const val FINANCIAL_CONNECTIONS_ACTIVITY =
            "com.stripe.android.financialconnections.FinancialConnectionsSheetActivity"
    }
}

private fun TestParameters.countDownLatch(): CountDownLatch? {
    return if (authorizationAction?.isConsideredDone == true) {
        CountDownLatch(1)
    } else {
        null
    }
}
