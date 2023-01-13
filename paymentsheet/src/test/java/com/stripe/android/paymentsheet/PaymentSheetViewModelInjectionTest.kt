package com.stripe.android.paymentsheet

import android.app.Application
import android.content.Context
import androidx.activity.ComponentActivity
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.stripe.android.model.PaymentIntentFixtures
import com.stripe.android.model.PaymentMethod
import com.stripe.android.ui.core.address.AddressRepository
import com.stripe.android.ui.core.forms.resources.LpmRepository
import com.stripe.android.utils.fakeCreationExtras
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@OptIn(FlowPreview::class)
@ExperimentalCoroutinesApi
@RunWith(RobolectricTestRunner::class)
internal class PaymentSheetViewModelInjectionTest : AbsPaymentSheetViewModelTestInjection() {

    @After
    override fun after() {
        super.after()
    }

    @Test
    fun `Factory gets initialized by Injector when Injector is available`() {
        ActivityScenario.launch(ComponentActivity::class.java).onActivity { activity ->
            val args = createArgs()
            val viewModel = createViewModel(args)

            val factory = PaymentSheetViewModel.Factory { args }
            registerViewModel(args.injectorKey, viewModel, lpmRepository, addressRepository)

            val creationExtras = activity.fakeCreationExtras()
            val result = factory.create(PaymentSheetViewModel::class.java, creationExtras)

            assertThat(result).isEqualTo(viewModel)
        }
    }

    @Test
    fun `Factory gets initialized with fallback when no Injector is available`() {
        ActivityScenario.launch(ComponentActivity::class.java).onActivity { activity ->
            val args = createArgs()
            val viewModel = createViewModel(args)

            val factory = PaymentSheetViewModel.Factory { args }
            val creationExtras = activity.fakeCreationExtras()
            val result = factory.create(PaymentSheetViewModel::class.java, creationExtras)

            assertThat(result).isNotEqualTo(viewModel)
        }
    }

    private fun createArgs(): PaymentSheetContract.Args {
        return PaymentSheetFixtures.ARGS_CUSTOMER_WITH_GOOGLEPAY.copy(
            injectorKey = "testInjectorKeyAddFragmentTest"
        )
    }

    private fun createViewModel(args: PaymentSheetContract.Args): PaymentSheetViewModel {
        return createViewModel(
            stripeIntent = PaymentIntentFixtures.PI_WITH_SHIPPING,
            customerRepositoryPMs = emptyList(),
            injectorKey = args.injectorKey
        )
    }

    companion object {
        val addressRepository =
            AddressRepository(ApplicationProvider.getApplicationContext<Context>().resources)
        val lpmRepository =
            LpmRepository(LpmRepository.LpmRepositoryArguments(ApplicationProvider.getApplicationContext<Application>().resources)).apply {
                this.forceUpdate(
                    listOf(
                        PaymentMethod.Type.Card.code,
                        PaymentMethod.Type.USBankAccount.code,
                        PaymentMethod.Type.SepaDebit.code,
                        PaymentMethod.Type.Bancontact.code
                    ),
                    null
                )
            }
    }
}
