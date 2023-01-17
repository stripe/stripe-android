package com.stripe.android.paymentsheet

import android.content.Context
import androidx.activity.ComponentActivity
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.stripe.android.ApiKeyFixtures
import com.stripe.android.PaymentConfiguration
import com.stripe.android.core.injection.DUMMY_INJECTOR_KEY
import com.stripe.android.model.PaymentIntentFixtures
import com.stripe.android.paymentsheet.model.PaymentIntentClientSecret
import com.stripe.android.paymentsheet.model.SavedSelection
import com.stripe.android.paymentsheet.state.PaymentSheetState
import com.stripe.android.ui.core.forms.resources.LpmRepository
import com.stripe.android.utils.FakeAndroidKeyStore
import com.stripe.android.utils.PaymentIntentFactory
import com.stripe.android.utils.fakeCreationExtras
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.robolectric.RobolectricTestRunner

@OptIn(FlowPreview::class)
@ExperimentalCoroutinesApi
@RunWith(RobolectricTestRunner::class)
internal class PaymentOptionsViewModelInjectionTest : BasePaymentOptionsViewModelInjectionTest() {

    private val context: Context = ApplicationProvider.getApplicationContext()

    private val paymentIntent = PaymentIntentFactory.create()
    private val lpmRepository = LpmRepository(LpmRepository.LpmRepositoryArguments(context.resources)).apply {
        this.update(paymentIntent, null)
    }

    @Before
    fun setup() {
        FakeAndroidKeyStore.setup()
        PaymentConfiguration.init(
            ApplicationProvider.getApplicationContext(),
            ApiKeyFixtures.FAKE_PUBLISHABLE_KEY
        )
    }

    @After
    fun cleanUp() {
        super.after()
    }

    @Test
    fun `Factory gets initialized by Injector when Injector is available`() {
        ActivityScenario.launch(ComponentActivity::class.java).onActivity { activity ->
            val args = createArgs()
            val viewModel = createViewModel(args)

            val factory = PaymentOptionsViewModel.Factory { args }
            registerViewModel(args.injectorKey, viewModel, lpmRepository)

            val creationExtras = activity.fakeCreationExtras()
            val result = factory.create(PaymentOptionsViewModel::class.java, creationExtras)

            assertThat(result).isEqualTo(viewModel)
        }
    }

    @Test
    fun `Factory gets initialized with fallback when no Injector is available`() {
        ActivityScenario.launch(ComponentActivity::class.java).onActivity { activity ->
            val args = createArgs()
            val viewModel = createViewModel(args)

            val factory = PaymentOptionsViewModel.Factory { args }

            val creationExtras = activity.fakeCreationExtras()
            val result = factory.create(PaymentOptionsViewModel::class.java, creationExtras)

            assertThat(result).isNotEqualTo(viewModel)
        }
    }

    private fun createArgs(): PaymentOptionContract.Args {
        return PaymentOptionContract.Args(
            state = PaymentSheetState.Full(
                stripeIntent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD,
                clientSecret = PaymentIntentClientSecret("secret"),
                customerPaymentMethods = emptyList(),
                savedSelection = SavedSelection.None,
                config = PaymentSheetFixtures.CONFIG_GOOGLEPAY,
                isGooglePayReady = false,
                newPaymentSelection = null,
                linkState = null,
            ),
            statusBarColor = PaymentSheetFixtures.STATUS_BAR_COLOR,
            injectorKey = DUMMY_INJECTOR_KEY,
            enableLogging = false,
            productUsage = mock()
        )
    }

    private fun createViewModel(args: PaymentOptionContract.Args): PaymentOptionsViewModel {
        return createViewModel(
            paymentMethods = args.state.customerPaymentMethods,
            injectorKey = args.injectorKey,
            args = args
        )
    }
}
