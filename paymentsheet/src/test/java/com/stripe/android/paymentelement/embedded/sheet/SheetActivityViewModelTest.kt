package com.stripe.android.paymentelement.embedded.sheet

import androidx.lifecycle.SavedStateHandle
import com.google.common.truth.Truth.assertThat
import com.stripe.android.model.PaymentMethodMessagePromotion
import com.stripe.android.paymentelement.AddressAutocompletePreview
import com.stripe.android.paymentelement.EmbeddedPaymentElement
import com.stripe.android.paymentelement.embedded.EmbeddedLaunchMode
import com.stripe.android.paymentsheet.PaymentSheetFixtures
import com.stripe.android.paymentsheet.PaymentSheetFixtures.PAYMENT_OPTIONS_CONTRACT_ARGS
import com.stripe.android.paymentsheet.analytics.EventReporter
import com.stripe.android.paymentsheet.analytics.FakeEventReporter
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.repositories.PaymentMethodMessagePromotionsHelper
import com.stripe.android.paymentsheet.state.PaymentElementLoader
import com.stripe.android.paymentsheet.utils.ViewModelStoreTestRule
import com.stripe.android.testing.CoroutineTestRule
import com.stripe.android.utils.FakePaymentElementLoader
import com.stripe.android.utils.FakePaymentMethodMessagePromotionsHelper
import org.junit.Rule
import org.junit.Test

@OptIn(AddressAutocompletePreview::class)
internal class SheetActivityViewModelTest {
    @get:Rule
    val coroutineTestRule = CoroutineTestRule()

    @get:Rule
    val viewModelStoreRule = ViewModelStoreTestRule()

    @Test
    fun `payment options arguments use the embedded payment options runtime`() {
        val viewModel = viewModelStoreRule.track(
            SheetActivityViewModel(
                args = SheetActivityArgs.PaymentOptions(PAYMENT_OPTIONS_CONTRACT_ARGS),
                loaderComponent = null,
                savedStateHandle = SavedStateHandle(),
            )
        )

        val ready = viewModel.state.value as SheetActivityViewModel.State.Ready
        assertThat(ready.args.launchMode).isEqualTo(EmbeddedLaunchMode.PaymentOptions)
        assertThat(ready.args.configuration.formSheetAction)
            .isEqualTo(EmbeddedPaymentElement.FormSheetAction.Continue)
        assertThat(ready.args.paymentMethodMetadata)
            .isEqualTo(PAYMENT_OPTIONS_CONTRACT_ARGS.state.paymentMethodMetadata)
    }

    @Test
    @Suppress("DEPRECATION")
    fun `payment sheet load uses the embedded complete runtime`() {
        val promotions = FakePaymentMethodMessagePromotionsHelper.promotions
        val loader = FakePaymentElementLoader(
            customer = PaymentSheetFixtures.EMPTY_CUSTOMER_STATE,
            paymentSelection = PaymentSelection.GooglePay,
        )
        val paymentSheetArgs = PaymentSheetFixtures.ARGS_CUSTOMER_WITH_GOOGLEPAY.copy(
            config = PaymentSheetFixtures.ARGS_CUSTOMER_WITH_GOOGLEPAY.config.newBuilder()
                .googlePlacesApiKey("places_key")
                .build(),
        )
        val viewModel = viewModelStoreRule.track(
            SheetActivityViewModel(
                args = SheetActivityArgs.PaymentSheet(paymentSheetArgs),
                loaderComponent = loaderComponent(loader, promotions),
                savedStateHandle = SavedStateHandle(),
            )
        )

        val ready = viewModel.state.value as SheetActivityViewModel.State.Ready
        assertThat(ready.args.launchMode).isEqualTo(EmbeddedLaunchMode.Complete)
        assertThat(ready.args.configuration.formSheetAction)
            .isEqualTo(EmbeddedPaymentElement.FormSheetAction.Confirm)
        assertThat(ready.args.selection).isEqualTo(PaymentSelection.GooglePay)
        assertThat(ready.args.customerState).isEqualTo(PaymentSheetFixtures.EMPTY_CUSTOMER_STATE)
        assertThat(ready.args.promotions).isEqualTo(promotions)
        assertThat(ready.args.configuration.googlePlacesApiKey).isEqualTo("places_key")
        assertThat(loader.lastIntegrationConfiguration)
            .isInstanceOf(PaymentElementLoader.Configuration.PaymentSheet::class.java)
    }

    @Test
    fun `payment sheet load failure is exposed to the activity`() {
        val viewModel = viewModelStoreRule.track(
            SheetActivityViewModel(
                args = SheetActivityArgs.PaymentSheet(PaymentSheetFixtures.ARGS_CUSTOMER_WITH_GOOGLEPAY),
                loaderComponent = loaderComponent(FakePaymentElementLoader(shouldFail = true), null),
                savedStateHandle = SavedStateHandle(),
            )
        )

        val failed = viewModel.state.value as SheetActivityViewModel.State.Failed
        assertThat(failed.error).hasMessageThat().isEqualTo("oh no")
    }

    private fun loaderComponent(
        loader: PaymentElementLoader,
        promotions: List<PaymentMethodMessagePromotion>?,
    ): SheetActivityLoaderComponent {
        return object : SheetActivityLoaderComponent {
            override val paymentElementLoader: PaymentElementLoader = loader
            override val promotionsHelper: PaymentMethodMessagePromotionsHelper =
                FakePaymentMethodMessagePromotionsHelper(promotions)
            override val eventReporter: EventReporter = FakeEventReporter()
        }
    }
}
