package com.stripe.android.paymentsheet

import android.app.Application
import android.content.Context
import androidx.activity.ComponentActivity
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.stripe.android.model.PaymentIntentFixtures
import com.stripe.android.model.PaymentMethod
import com.stripe.android.ui.core.forms.resources.LpmRepository
import com.stripe.android.uicore.address.AddressRepository
import com.stripe.android.utils.LpmUpdateParamsFactory
import com.stripe.android.utils.fakeCreationExtras
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class PaymentSheetViewModelInjectionTest : BasePaymentSheetViewModelInjectionTest() {

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
        val addressRepository = AddressRepository(
            resources = ApplicationProvider.getApplicationContext<Context>().resources
        )

        val lpmRepository = LpmRepository(
            arguments = LpmRepository.LpmRepositoryArguments(
                resources = ApplicationProvider.getApplicationContext<Application>().resources,
            ),
        ).apply {
            update(
                LpmUpdateParamsFactory.create(
                    paymentMethodTypes = listOf(
                        PaymentMethod.Type.Card.code,
                        PaymentMethod.Type.USBankAccount.code,
                        PaymentMethod.Type.SepaDebit.code,
                        PaymentMethod.Type.Bancontact.code
                    ),
                )
            )
        }
    }
}
