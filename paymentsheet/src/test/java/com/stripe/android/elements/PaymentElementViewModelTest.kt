package com.stripe.android.elements

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.stripe.android.model.CardBrand
import com.stripe.android.model.PaymentIntentFixtures
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.PaymentMethodCreateParams
import com.stripe.android.paymentsheet.PaymentSheetFixtures.CONFIG_CUSTOMER_WITH_GOOGLEPAY
import com.stripe.android.paymentsheet.R
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.paymentdatacollection.FormFragmentArguments
import com.stripe.android.ui.core.forms.resources.LpmRepository
import kotlinx.coroutines.MainScope
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class PaymentElementViewModelTest {
    private val config = PaymentElementConfig(
        paymentSheetConfig = CONFIG_CUSTOMER_WITH_GOOGLEPAY,
        stripeIntent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD,
        merchantName = "Merchant",
        initialSelection = null
    )

    @Test
    fun `FormArguments newLPM with customer requested save and Generic`() {
        val paymentIntent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD.copy(
            paymentMethodTypes = listOf("card", "bancontact")
        )
        val paymentMethodCreateParams = PaymentMethodCreateParams.createWithOverride(
            code = "bancontact",
            requiresMandate = true,
            overrideParamMap = mapOf(
                "type" to "bancontact",
                "billing_details" to mapOf(
                    "name" to "Jenny Rosen"
                )
            ),
            productUsage = emptySet()
        )

        val viewModel = createViewModel(
            config.copy(
                stripeIntent = paymentIntent,
                initialSelection = PaymentSelection.New.GenericPaymentMethod(
                    context.getString(R.string.stripe_paymentsheet_payment_method_bancontact),
                    R.drawable.stripe_ic_paymentsheet_pm_bancontact,
                    paymentMethodCreateParams,
                    PaymentSelection.CustomerRequestedSave.NoRequest
                )
            )
        )
        val actualFromArguments = viewModel.formArgumentsFlow.value

        assertThat(actualFromArguments.initialPaymentMethodCreateParams)
            .isEqualTo(paymentMethodCreateParams)
        assertThat(actualFromArguments.showCheckbox)
            .isFalse()
        assertThat(actualFromArguments.showCheckboxControlledFields)
            .isFalse()
    }

    @Test
    fun `getFormArguments newLPM WITH customer requested save and Card`() {
        val actualFromArguments = testCardFormArguments(
            PaymentSelection.CustomerRequestedSave.RequestReuse
        )

        assertThat(actualFromArguments.showCheckboxControlledFields)
            .isTrue()
    }

    @Test
    fun `getFormArguments newLPM WITH NO customer requested save and Card`() {
        val actualFromArguments = testCardFormArguments(
            PaymentSelection.CustomerRequestedSave.NoRequest
        )

        assertThat(actualFromArguments.showCheckboxControlledFields)
            .isFalse()
    }

    private fun testCardFormArguments(customerReuse: PaymentSelection.CustomerRequestedSave): FormFragmentArguments {
        val paymentIntent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD.copy(
            paymentMethodTypes = listOf("card", "bancontact")
        )
        val paymentMethodCreateParams = PaymentMethodCreateParams.createWithOverride(
            code = "card",
            requiresMandate = false,
            overrideParamMap = mapOf(
                "type" to "card",
                "card" to mapOf(
                    "cvc" to "123",
                    "number" to "4242424242424242",
                    "exp_date" to "1250"
                ),
                "billing_details" to mapOf(
                    "address" to mapOf(
                        "country" to "Jenny Rosen"
                    )
                )
            ),
            productUsage = emptySet()
        )

        val viewModel = createViewModel(
            config.copy(
                stripeIntent = paymentIntent,
                initialSelection = PaymentSelection.New.Card(
                    paymentMethodCreateParams,
                    CardBrand.Visa,
                    customerReuse
                )
            )
        )
        val actualFromArguments = viewModel.formArgumentsFlow.value

        assertThat(actualFromArguments.initialPaymentMethodCreateParams)
            .isEqualTo(paymentMethodCreateParams)

        return actualFromArguments
    }

    @Test
    fun `when payment intent is off session then form arguments are set correctly`() {
        val stripeIntent = PaymentIntentFixtures.PI_OFF_SESSION
        val viewModel = createViewModel(config.copy(stripeIntent = stripeIntent))
        val actualFromArguments = viewModel.formArgumentsFlow.value

        assertThat(actualFromArguments.paymentMethodCode)
            .isEqualTo(LpmRepository.HardcodedCard.code)
        assertThat(actualFromArguments.showCheckbox).isFalse()
        assertThat(actualFromArguments.showCheckboxControlledFields).isTrue()
    }

    private fun createViewModel(
        paymentElementConfig: PaymentElementConfig = config,
        supportedPaymentMethods: List<LpmRepository.SupportedPaymentMethod> = listOf(
            LpmRepository.HardcodedCard,
            Bancontact,
            SepaDebit
        )
    ) = PaymentElementViewModel(
        supportedPaymentMethods = supportedPaymentMethods,
        paymentElementConfig = paymentElementConfig,
        context = context,
        lifecycleScope = MainScope()
    )

    companion object {
        val context: Context = ApplicationProvider.getApplicationContext()
        val lpmRepository =
            LpmRepository(LpmRepository.LpmRepositoryArguments(context.resources)).apply {
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
        val Bancontact = lpmRepository.fromCode("bancontact")!!
        val SepaDebit = lpmRepository.fromCode("sepa_debit")!!
    }
}
