package com.stripe.android.paymentelement.embedded.sheet

import android.app.Application
import android.os.Bundle
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso
import androidx.test.espresso.Espresso.onIdle
import com.google.common.truth.Truth.assertThat
import com.stripe.android.isInstanceOf
import com.stripe.android.link.LinkAccountUpdate
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadataFactory
import com.stripe.android.networktesting.NetworkRule
import com.stripe.android.paymentelement.EmbeddedPaymentElement
import com.stripe.android.paymentelement.embedded.EmbeddedActivityArgs
import com.stripe.android.paymentelement.embedded.EmbeddedActivityResult
import com.stripe.android.paymentelement.embedded.EmbeddedLaunchMode
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.PaymentSheetFixtures
import com.stripe.android.testing.CoroutineTestRule
import com.stripe.android.testing.PaymentConfigurationTestRule
import com.stripe.android.testing.createComposeCleanupRule
import com.stripe.android.uicore.elements.bottomsheet.BottomSheetContentTestTag
import com.stripe.paymentelementtestpages.FormPage
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class LoadingEmbeddedSheetActivityTest {
    private val applicationContext = ApplicationProvider.getApplicationContext<Application>()
    private val composeTestRule = createEmptyComposeRule()
    private val formPage = FormPage(composeTestRule)

    @get:Rule
    val ruleChain: RuleChain = RuleChain
        .outerRule(composeTestRule)
        .around(createComposeCleanupRule())
        .around(CoroutineTestRule())
        .around(NetworkRule())
        .around(PaymentConfigurationTestRule(applicationContext))

    @Test
    fun `loading without state survives recreation and can be cancelled`() = launch(
        paymentMethodMetadata = null,
        configuration = null,
        presentationState = EmbeddedActivityArgs.PresentationState.Loading,
    ) { scenario ->
        composeTestRule.onNodeWithTag(EMBEDDED_SHEET_LOADING_TEST_TAG).assertIsDisplayed()
        scenario.recreate()
        onIdle()
        composeTestRule.onNodeWithTag(EMBEDDED_SHEET_LOADING_TEST_TAG).assertIsDisplayed()

        Espresso.pressBack()
        onIdle()

        val result = EmbeddedSheetContract.parseResult(scenario.result.resultCode, scenario.result.resultData)
        assertThat(result).isInstanceOf<EmbeddedActivityResult.Cancelled>()
        assertThat(result.launchMode).isEqualTo(EmbeddedLaunchMode.PaymentOptions)
    }

    @Test
    fun `failed intent closes loading sheet without state`() = launch(
        paymentMethodMetadata = null,
        configuration = null,
        presentationState = EmbeddedActivityArgs.PresentationState.Loading,
    ) { scenario ->
        composeTestRule.onNodeWithTag(EMBEDDED_SHEET_LOADING_TEST_TAG).assertIsDisplayed()
        val failedIntent = EmbeddedSheetContract.createIntent(
            applicationContext,
            createArgs(
                paymentMethodMetadata = null,
                configuration = null,
                presentationState = EmbeddedActivityArgs.PresentationState.Failed,
            ),
        )

        scenario.onActivity { activity -> activity.onNewIntent(failedIntent) }
        onIdle()

        val result = EmbeddedSheetContract.parseResult(scenario.result.resultCode, scenario.result.resultData)
        assertThat(result).isEqualTo(EmbeddedActivityResult.Error(launchMode = EmbeddedLaunchMode.PaymentOptions))
    }

    @Test
    fun `failed initial intent closes sheet without state`() = launch(
        paymentMethodMetadata = null,
        configuration = null,
        presentationState = EmbeddedActivityArgs.PresentationState.Failed,
    ) { scenario ->
        val result = EmbeddedSheetContract.parseResult(scenario.result.resultCode, scenario.result.resultData)
        assertThat(result).isEqualTo(EmbeddedActivityResult.Error(launchMode = EmbeddedLaunchMode.PaymentOptions))
    }

    @Test
    fun `loading renders and cancellation returns PaymentOptions`() = launch(
        presentationState = EmbeddedActivityArgs.PresentationState.Loading,
    ) { scenario ->
        composeTestRule.onNodeWithTag(EMBEDDED_SHEET_LOADING_TEST_TAG).assertIsDisplayed()

        Espresso.pressBack()
        onIdle()

        val result = EmbeddedSheetContract.parseResult(
            scenario.result.resultCode,
            scenario.result.resultData,
        ) as EmbeddedActivityResult.Cancelled
        assertThat(result.customerState).isEqualTo(PaymentSheetFixtures.EMPTY_CUSTOMER_STATE)
        assertThat(result.linkAccountInfo.lastUpdateReason)
            .isEqualTo(LinkAccountUpdate.Value.UpdateReason.LoggedOut)
        assertThat(result.launchMode).isEqualTo(EmbeddedLaunchMode.PaymentOptions)
    }

    @Test
    fun `recreated loading activity remains loading`() = launch(
        presentationState = EmbeddedActivityArgs.PresentationState.Loading,
    ) { scenario ->
        composeTestRule.onNodeWithTag(EMBEDDED_SHEET_LOADING_TEST_TAG).assertIsDisplayed()

        scenario.recreate()
        onIdle()

        scenario.onActivity { activity ->
            assertThat(activity.isFinishing).isFalse()
        }
        composeTestRule.onNodeWithTag(EMBEDDED_SHEET_LOADING_TEST_TAG).assertIsDisplayed()
    }

    @Test
    fun `ready intent updates loading content without replacing composition and survives recreation`() = launch(
        paymentMethodMetadata = null,
        configuration = null,
        presentationState = EmbeddedActivityArgs.PresentationState.Loading,
    ) { scenario ->
        val readyIntent = EmbeddedSheetContract.createIntent(
            applicationContext,
            createArgs(
                paymentMethodMetadata = PaymentMethodMetadataFactory.create(
                    paymentMethodLayout = PaymentSheet.PaymentMethodLayout.Horizontal,
                ),
                presentationState = EmbeddedActivityArgs.PresentationState.Ready,
            ),
        )

        scenario.recreate()
        onIdle()
        composeTestRule.onNodeWithTag(EMBEDDED_SHEET_LOADING_TEST_TAG).assertIsDisplayed()

        val initialBottomSheetNodeId = composeTestRule
            .onNodeWithTag(BottomSheetContentTestTag)
            .fetchSemanticsNode()
            .id
        scenario.onActivity { activity ->
            activity.onNewIntent(readyIntent)
        }
        composeTestRule.waitForIdle()

        formPage.waitUntilVisible()
        composeTestRule.onNodeWithTag(EMBEDDED_SHEET_LOADING_TEST_TAG).assertDoesNotExist()
        val currentBottomSheetNodeId = composeTestRule
            .onNodeWithTag(BottomSheetContentTestTag)
            .fetchSemanticsNode()
            .id
        assertThat(currentBottomSheetNodeId).isEqualTo(initialBottomSheetNodeId)

        scenario.recreate()
        onIdle()

        formPage.waitUntilVisible()
        composeTestRule.onNodeWithTag(EMBEDDED_SHEET_LOADING_TEST_TAG).assertDoesNotExist()
    }

    private fun launch(
        paymentMethodMetadata: PaymentMethodMetadata? = PaymentMethodMetadataFactory.create(
            paymentMethodLayout = PaymentSheet.PaymentMethodLayout.Vertical,
        ),
        configuration: EmbeddedPaymentElement.Configuration? =
            EmbeddedPaymentElement.Configuration.Builder("Example, Inc.").build(),
        presentationState: EmbeddedActivityArgs.PresentationState,
        block: (ActivityScenario<EmbeddedSheetActivity>) -> Unit,
    ) {
        ActivityScenario.launchActivityForResult<EmbeddedSheetActivity>(
            EmbeddedSheetContract.createIntent(
                context = applicationContext,
                input = createArgs(
                    paymentMethodMetadata = paymentMethodMetadata,
                    configuration = configuration,
                    presentationState = presentationState,
                ),
            )
        ).use(block)
    }

    private fun createArgs(
        paymentMethodMetadata: PaymentMethodMetadata? = PaymentMethodMetadataFactory.create(
            paymentMethodLayout = PaymentSheet.PaymentMethodLayout.Vertical,
        ),
        presentationState: EmbeddedActivityArgs.PresentationState,
        configuration: EmbeddedPaymentElement.Configuration? =
            EmbeddedPaymentElement.Configuration.Builder("Example, Inc.").build(),
    ): EmbeddedActivityArgs {
        return EmbeddedActivityArgs(
            paymentMethodMetadata = paymentMethodMetadata,
            configuration = configuration,
            productUsage = setOf("EmbeddedPaymentElement"),
            statusBarColor = null,
            paymentElementCallbackIdentifier = "PaymentOptionsTestIdentifier",
            selection = null,
            previousNewSelections = Bundle(),
            customerState = PaymentSheetFixtures.EMPTY_CUSTOMER_STATE,
            linkAccountInfo = LinkAccountUpdate.Value(
                account = null,
                lastUpdateReason = LinkAccountUpdate.Value.UpdateReason.LoggedOut,
            ),
            promotions = emptyList(),
            launchMode = EmbeddedLaunchMode.PaymentOptions,
            presentationState = presentationState,
        )
    }
}
