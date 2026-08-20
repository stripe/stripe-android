package com.stripe.android.paymentsheet

import android.content.Context
import androidx.core.os.BundleCompat
import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import com.stripe.android.ApiKeyFixtures
import com.stripe.android.PaymentConfiguration
import com.stripe.android.link.LinkAccountUpdate
import com.stripe.android.link.TestFactory
import com.stripe.android.paymentelement.embedded.sheet.EmbeddedSheetActivity
import com.stripe.android.paymentelement.embedded.sheet.SheetActivityArgs
import com.stripe.android.paymentsheet.state.PaymentElementLoader
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class SheetActivityContractTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()

    @Test
    fun `PaymentSheet contract launches the shared sheet activity with its original args`() {
        val args = PaymentSheetFixtures.ARGS_CUSTOMER_WITH_GOOGLEPAY

        val intent = PaymentSheetContract().createIntent(context, args)

        assertThat(intent.component?.className).isEqualTo(EmbeddedSheetActivity::class.java.name)
        val hostArgs = BundleCompat.getParcelable(
            requireNotNull(intent.extras),
            SheetActivityArgs.EXTRA_ARGS,
            SheetActivityArgs::class.java,
        )
        assertThat((hostArgs as? SheetActivityArgs.PaymentSheet)?.args).isEqualTo(args)
    }

    @Test
    fun `Payment options contract launches the shared sheet activity with its original args`() {
        val args = PaymentSheetFixtures.PAYMENT_OPTIONS_CONTRACT_ARGS

        val intent = PaymentOptionContract().createIntent(context, args)

        assertThat(intent.component?.className).isEqualTo(EmbeddedSheetActivity::class.java.name)
        val hostArgs = BundleCompat.getParcelable(
            requireNotNull(intent.extras),
            SheetActivityArgs.EXTRA_ARGS,
            SheetActivityArgs::class.java,
        )
        assertThat((hostArgs as? SheetActivityArgs.PaymentOptions)?.args).isEqualTo(args)
    }

    @Test
    fun `shared sheet activity returns a PaymentSheet failure for invalid launch args`() {
        PaymentConfiguration.init(context, ApiKeyFixtures.FAKE_PUBLISHABLE_KEY)
        val intent = PaymentSheetContract().createIntent(
            context,
            PaymentSheetContract.Args(
                initializationMode = PaymentElementLoader.InitializationMode.PaymentIntent(clientSecret = ""),
                config = PaymentSheet.Configuration(merchantDisplayName = "Merchant, Inc."),
                paymentElementCallbackIdentifier = PaymentSheetFixtures.PAYMENT_SHEET_CALLBACK_TEST_IDENTIFIER,
                statusBarColor = null,
            ),
        )

        ActivityScenario.launchActivityForResult<EmbeddedSheetActivity>(intent).use { scenario ->
            assertThat(scenario.state).isEqualTo(Lifecycle.State.DESTROYED)
            assertThat(
                PaymentSheetContract().parseResult(scenario.result.resultCode, scenario.result.resultData)
            ).isInstanceOf(PaymentSheetResult.Failed::class.java)
        }
    }

    @Test
    fun `payment options initializes the shared Link account`() {
        PaymentConfiguration.init(context, ApiKeyFixtures.FAKE_PUBLISHABLE_KEY)
        val linkAccountInfo = LinkAccountUpdate.Value(TestFactory.LINK_ACCOUNT)
        val intent = PaymentOptionContract().createIntent(
            context,
            PaymentSheetFixtures.PAYMENT_OPTIONS_CONTRACT_ARGS.copy(
                linkAccountInfo = linkAccountInfo,
                productUsage = setOf("FlowController"),
            ),
        )

        ActivityScenario.launch<EmbeddedSheetActivity>(intent).use { scenario ->
            InstrumentationRegistry.getInstrumentation().waitForIdleSync()
            scenario.onActivity { activity ->
                assertThat(activity.linkAccountHolder.linkAccountInfo.value).isEqualTo(linkAccountInfo)
            }
        }
    }
}
