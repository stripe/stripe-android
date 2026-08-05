package com.stripe.android.paymentelement.embedded.sheet

import android.app.Activity
import android.app.Application
import android.os.Bundle
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso
import androidx.test.espresso.Espresso.onIdle
import com.google.common.truth.Truth.assertThat
import com.stripe.android.isInstanceOf
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadataFactory
import com.stripe.android.model.PaymentMethodFixtures
import com.stripe.android.paymentelement.EmbeddedPaymentElement
import com.stripe.android.paymentelement.embedded.EmbeddedActivityArgs
import com.stripe.android.paymentelement.embedded.EmbeddedActivityResult
import com.stripe.android.paymentelement.embedded.EmbeddedLaunchMode
import com.stripe.android.paymentelement.embedded.stashNewSelection
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.PaymentSheetFixtures
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.testing.PaymentConfigurationTestRule
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class PaymentOptionsEmbeddedSheetActivityTest {
    private val applicationContext = ApplicationProvider.getApplicationContext<Application>()
    private val composeTestRule = createAndroidComposeRule<EmbeddedSheetActivity>()

    @get:Rule
    val ruleChain: RuleChain = RuleChain
        .outerRule(composeTestRule)
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
            EmbeddedLaunchMode.PaymentOptions(paymentMethodLayout = PaymentSheet.PaymentMethodLayout.Vertical)
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
                    shouldInvokeSelectionCallback = false,
                    launchMode = EmbeddedLaunchMode.PaymentOptions(
                        paymentMethodLayout = PaymentSheet.PaymentMethodLayout.Vertical,
                    ),
                )
            )
        }

        onIdle()

        assertThat(scenario.result.resultCode).isEqualTo(Activity.RESULT_OK)
        val result = EmbeddedSheetContract.parseResult(scenario.result.resultCode, scenario.result.resultData)
        assertThat(result).isInstanceOf<EmbeddedActivityResult.Complete>()
        val complete = result as EmbeddedActivityResult.Complete
        assertThat(complete.launchMode).isEqualTo(
            EmbeddedLaunchMode.PaymentOptions(paymentMethodLayout = PaymentSheet.PaymentMethodLayout.Vertical)
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
            EmbeddedLaunchMode.PaymentOptions(paymentMethodLayout = PaymentSheet.PaymentMethodLayout.Vertical)
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
    fun `cancelled result contains customer state`() = launch { scenario ->
        Espresso.pressBack()

        onIdle()

        val result = EmbeddedSheetContract.parseResult(scenario.result.resultCode, scenario.result.resultData)
        val cancelled = result as EmbeddedActivityResult.Cancelled
        assertThat(cancelled.customerState).isNotNull()
    }

    private fun launch(
        selection: PaymentSelection? = null,
        previousNewSelections: Bundle = Bundle(),
        block: (ActivityScenario<EmbeddedSheetActivity>) -> Unit,
    ) {
        ActivityScenario.launchActivityForResult<EmbeddedSheetActivity>(
            EmbeddedSheetContract.createIntent(
                context = applicationContext,
                input = EmbeddedActivityArgs(
                    paymentMethodMetadata = PaymentMethodMetadataFactory.create(),
                    configuration = EmbeddedPaymentElement.Configuration.Builder("Example, Inc.").build(),
                    statusBarColor = null,
                    paymentElementCallbackIdentifier = "PaymentOptionsTestIdentifier",
                    selection = selection,
                    previousNewSelections = previousNewSelections,
                    customerState = PaymentSheetFixtures.EMPTY_CUSTOMER_STATE,
                    promotion = null,
                    launchMode = EmbeddedLaunchMode.PaymentOptions(
                        paymentMethodLayout = PaymentSheet.PaymentMethodLayout.Vertical,
                    ),
                ),
            )
        ).use { scenario ->
            block(scenario)
        }
    }
}
