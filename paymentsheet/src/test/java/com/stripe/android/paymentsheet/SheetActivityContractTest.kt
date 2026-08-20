package com.stripe.android.paymentsheet

import android.content.Context
import androidx.core.os.BundleCompat
import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.stripe.android.ApiKeyFixtures
import com.stripe.android.PaymentConfiguration
import com.stripe.android.paymentelement.embedded.EmbeddedActivityArgs
import com.stripe.android.paymentelement.embedded.EmbeddedLaunchMode
import com.stripe.android.paymentelement.embedded.sheet.EmbeddedSheetActivity
import com.stripe.android.paymentelement.embedded.sheet.finishLoading
import com.stripe.android.paymentsheet.state.PaymentElementLoader
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf

@RunWith(RobolectricTestRunner::class)
internal class SheetActivityContractTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()

    @Test
    fun `PaymentSheet contract launches the shared sheet activity with its original args`() {
        val args = PaymentSheetFixtures.ARGS_CUSTOMER_WITH_GOOGLEPAY

        val intent = PaymentSheetContract().createIntent(context, args)

        assertThat(intent.component?.className).isEqualTo(EmbeddedSheetActivity::class.java.name)
        val embeddedArgs = BundleCompat.getParcelable(
            requireNotNull(intent.extras),
            EmbeddedActivityArgs.EXTRA_ARGS,
            EmbeddedActivityArgs::class.java,
        )
        assertThat(embeddedArgs?.launchMode).isEqualTo(EmbeddedLaunchMode.Complete)
        assertThat(embeddedArgs?.presentationState)
            .isEqualTo(EmbeddedActivityArgs.PresentationState.Loading)
        assertThat(embeddedArgs?.paymentMethodMetadata).isNull()
        assertThat(embeddedArgs?.activityConfiguration)
            .isEqualTo(EmbeddedActivityArgs.ActivityConfiguration.PaymentSheet(args))
    }

    @Test
    fun `Payment options contract launches the shared sheet activity with its original args`() {
        val args = PaymentSheetFixtures.PAYMENT_OPTIONS_CONTRACT_ARGS

        val intent = PaymentOptionContract().createIntent(context, args)

        assertThat(intent.component?.className).isEqualTo(EmbeddedSheetActivity::class.java.name)
        val embeddedArgs = BundleCompat.getParcelable(
            requireNotNull(intent.extras),
            EmbeddedActivityArgs.EXTRA_ARGS,
            EmbeddedActivityArgs::class.java,
        )
        assertThat(embeddedArgs?.launchMode).isEqualTo(EmbeddedLaunchMode.PaymentOptions)
        assertThat(embeddedArgs?.presentationState)
            .isEqualTo(EmbeddedActivityArgs.PresentationState.Ready)
        assertThat(embeddedArgs?.paymentMethodMetadata).isEqualTo(args.state.paymentMethodMetadata)
        assertThat(embeddedArgs?.activityConfiguration)
            .isEqualTo(
                EmbeddedActivityArgs.ActivityConfiguration.PaymentOptions(
                    initialSelection = args.state.paymentSelection,
                    initialLinkAccount = args.linkAccountInfo,
                    productUsageTokens = args.productUsage,
                )
            )
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
    fun `cancelling the loading presentation returns a canceled PaymentSheet result`() {
        val args = PaymentSheetFixtures.ARGS_CUSTOMER_WITH_GOOGLEPAY.asEmbeddedActivityArgs()
        val activity = Robolectric.buildActivity(EmbeddedSheetActivity::class.java).get()

        activity.finishLoading(args)

        val shadowActivity = shadowOf(activity)
        val result = PaymentSheetContract().parseResult(
            shadowActivity.resultCode,
            shadowActivity.resultIntent,
        )
        assertThat(result).isInstanceOf(PaymentSheetResult.Canceled::class.java)
    }
}
