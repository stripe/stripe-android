package com.stripe.android.paymentelement.embedded.sheet

import android.app.Activity
import android.app.Application
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso
import androidx.test.espresso.Espresso.onIdle
import com.google.common.truth.Truth.assertThat
import com.stripe.android.isInstanceOf
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadataFactory
import com.stripe.android.paymentelement.EmbeddedPaymentElement
import com.stripe.android.paymentelement.embedded.EmbeddedActivityArgs
import com.stripe.android.paymentelement.embedded.EmbeddedActivityResult
import com.stripe.android.paymentelement.embedded.EmbeddedLaunchMode
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
        assertThat(cancelled.launchMode).isEqualTo(EmbeddedLaunchMode.PaymentOptions)
    }

    @Test
    fun `when SheetActivityStateHolder has result, activity finishes with that result`() = launch { scenario ->
        scenario.onActivity { activity ->
            activity.sheetActivityStateHolder.setResult(
                EmbeddedActivityResult.Complete(
                    selection = null,
                    hasBeenConfirmed = false,
                    customerState = null,
                    shouldInvokeSelectionCallback = false,
                    checkoutSessionResponse = null,
                    launchMode = EmbeddedLaunchMode.PaymentOptions,
                )
            )
        }

        onIdle()

        assertThat(scenario.result.resultCode).isEqualTo(Activity.RESULT_OK)
        val result = EmbeddedSheetContract.parseResult(scenario.result.resultCode, scenario.result.resultData)
        assertThat(result).isInstanceOf<EmbeddedActivityResult.Complete>()
        val complete = result as EmbeddedActivityResult.Complete
        assertThat(complete.launchMode).isEqualTo(EmbeddedLaunchMode.PaymentOptions)
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
        assertThat(cancelled.launchMode).isEqualTo(EmbeddedLaunchMode.PaymentOptions)
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
                    customerState = PaymentSheetFixtures.EMPTY_CUSTOMER_STATE,
                    promotion = null,
                    launchMode = EmbeddedLaunchMode.PaymentOptions,
                ),
            )
        ).use { scenario ->
            block(scenario)
        }
    }
}
