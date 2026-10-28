package com.stripe.android.paymentelement.embedded.sheet

import android.app.Activity
import android.os.Bundle
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import com.google.common.truth.Truth.assertThat
import com.stripe.android.link.LinkAccountUpdate
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadataFactory
import com.stripe.android.paymentelement.EmbeddedPaymentElement
import com.stripe.android.paymentelement.embedded.EmbeddedActivityArgs
import com.stripe.android.paymentelement.embedded.EmbeddedActivityResult
import com.stripe.android.paymentelement.embedded.EmbeddedLaunchMode
import com.stripe.android.paymentsheet.PaymentSheetFixtures
import com.stripe.android.testing.CoroutineTestRule
import com.stripe.android.testing.createComposeCleanupRule
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.android.controller.ActivityController

@RunWith(RobolectricTestRunner::class)
internal class EmbeddedSheetActivityResultTest {
    @get:Rule
    val composeRule = createEmptyComposeRule()

    @get:Rule
    val composeCleanupRule = createComposeCleanupRule()

    @get:Rule
    val coroutineTestRule = CoroutineTestRule(UnconfinedTestDispatcher())

    @Test
    fun `FlowController sets result before finishing`() = runScenario {
        activity.finishWithResult(result)

        assertThat(activity.isFinishing).isFalse()
        assertResult(result)
    }

    @Test
    fun `duplicate request does not replace pending result`() = runScenario {
        activity.finishWithResult(result)
        activity.finishWithResult(EmbeddedActivityResult.Error(EmbeddedLaunchMode.PaymentOptions))

        assertThat(activity.isFinishing).isFalse()
        assertResult(result)
    }

    @Test
    fun `pending result survives recreation and rejects duplicate request`() = runScenario {
        activity.finishWithResult(result)
        val savedState = Bundle()
        controller.saveInstanceState(savedState).destroy()

        Robolectric.buildActivity(EmbeddedSheetActivity::class.java, activity.intent)
            .create(savedState).use { restoredController ->
                val restoredActivity = restoredController.get()
                restoredActivity.finishWithResult(EmbeddedActivityResult.Error(EmbeddedLaunchMode.PaymentOptions))

                assertThat(restoredActivity.isFinishing).isFalse()
                assertThat(shadowOf(restoredActivity).resultCode).isEqualTo(Activity.RESULT_OK)
                assertThat(EmbeddedActivityResult.fromIntent(shadowOf(restoredActivity).resultIntent)).isEqualTo(result)

                restoredController.start().resume().visible()
                composeRule.waitForIdle()

                assertThat(restoredActivity.isFinishing).isTrue()
                assertThat(EmbeddedActivityResult.fromIntent(shadowOf(restoredActivity).resultIntent)).isEqualTo(result)
            }
    }

    @Test
    fun `regular embedded finishes immediately`() = runScenario(EmbeddedLaunchMode.Manage) {
        activity.finishWithResult(result)

        assertThat(activity.isFinishing).isTrue()
        assertResult(result)
    }

    @Test
    fun `duplicate request does not replace result after finishing`() = runScenario(EmbeddedLaunchMode.Manage) {
        activity.finishWithResult(result)
        activity.finishWithResult(EmbeddedActivityResult.Error(EmbeddedLaunchMode.Manage))

        assertResult(result)
    }

    private fun runScenario(
        launchMode: EmbeddedLaunchMode = EmbeddedLaunchMode.PaymentOptions,
        block: Scenario.() -> Unit,
    ) {
        val args = EmbeddedActivityArgs(
            paymentMethodMetadata = PaymentMethodMetadataFactory.create(),
            configuration = EmbeddedPaymentElement.Configuration.Builder("Example, Inc.").build(),
            productUsage = setOf("EmbeddedPaymentElement"),
            paymentElementCallbackIdentifier = "EmbeddedSheetActivityResultTest",
            statusBarColor = null,
            selection = null,
            previousNewSelections = Bundle(),
            customerState = PaymentSheetFixtures.EMPTY_CUSTOMER_STATE,
            linkAccountInfo = LinkAccountUpdate.Value(null),
            promotions = emptyList(),
            launchMode = launchMode,
            presentationState = EmbeddedActivityArgs.PresentationState.Loading,
        )
        val intent = EmbeddedSheetContract.createIntent(
            context = RuntimeEnvironment.getApplication(),
            input = args,
        )
        // Keep the activity below STARTED so Compose cannot consume the pending result before it is saved.
        Robolectric.buildActivity(EmbeddedSheetActivity::class.java, intent).create().use { controller ->
            Scenario(
                controller = controller,
                result = EmbeddedActivityResult.Cancelled(
                    customerState = args.customerState,
                    linkAccountInfo = args.linkAccountInfo,
                    launchMode = launchMode,
                ),
            ).block()
        }
    }

    private class Scenario(
        val controller: ActivityController<EmbeddedSheetActivity>,
        val result: EmbeddedActivityResult,
    ) {
        val activity: EmbeddedSheetActivity = controller.get()

        fun assertResult(expected: EmbeddedActivityResult) {
            assertThat(shadowOf(activity).resultCode).isEqualTo(Activity.RESULT_OK)
            assertThat(EmbeddedActivityResult.fromIntent(shadowOf(activity).resultIntent)).isEqualTo(expected)
        }
    }
}
