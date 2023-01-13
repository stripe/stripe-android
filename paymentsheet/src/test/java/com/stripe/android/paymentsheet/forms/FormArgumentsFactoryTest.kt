package com.stripe.android.paymentsheet.forms

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.stripe.android.model.CardBrand
import com.stripe.android.model.PaymentIntent
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.PaymentMethodCreateParams
import com.stripe.android.paymentsheet.PaymentSheetFixtures
import com.stripe.android.paymentsheet.R
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.paymentdatacollection.FormFragmentArguments
import com.stripe.android.ui.core.Amount
import com.stripe.android.ui.core.forms.resources.LpmRepository
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class FormArgumentsFactoryTest {

    private val resources = ApplicationProvider.getApplicationContext<Application>().resources

    private val lpmRepository = LpmRepository(
        LpmRepository.LpmRepositoryArguments(resources)
    ).apply {
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

    @Test
    fun `getFormArguments newLPM with customer requested save and Generic`() {
        val paymentIntent = mock<PaymentIntent>().also {
            whenever(it.paymentMethodTypes).thenReturn(listOf("card", "bancontact"))
        }

        val paymentMethodCreateParams = PaymentMethodCreateParams.createWithOverride(
            code = "bancontact",
            requiresMandate = true,
            overrideParamMap = mapOf(
                "type" to "bancontact",
                "billing_details" to mapOf("name" to "Jenny Rosen"),
            ),
            productUsage = emptySet(),
        )

        val actualArgs = FormArgumentsFactory.create(
            showPaymentMethod = lpmRepository.fromCode("bancontact")!!,
            stripeIntent = paymentIntent,
            config = PaymentSheetFixtures.CONFIG_MINIMUM,
            merchantName = PaymentSheetFixtures.MERCHANT_DISPLAY_NAME,
            amount = Amount(50, "USD"),
            newLpm = PaymentSelection.New.GenericPaymentMethod(
                labelResource = resources.getString(R.string.stripe_paymentsheet_payment_method_bancontact),
                iconResource = R.drawable.stripe_ic_paymentsheet_pm_bancontact,
                lightThemeIconUrl = null,
                darkThemeIconUrl = null,
                paymentMethodCreateParams = paymentMethodCreateParams,
                customerRequestedSave = PaymentSelection.CustomerRequestedSave.NoRequest,
            ),
        )

        assertThat(actualArgs.initialPaymentMethodCreateParams).isEqualTo(paymentMethodCreateParams)
        assertThat(actualArgs.showCheckbox).isFalse()
        assertThat(actualArgs.showCheckboxControlledFields).isFalse()
    }

    @Test
    fun `getFormArguments newLPM WITH customer requested save and Card`() {
        val actualFromArguments = testCardFormArguments(
            PaymentSelection.CustomerRequestedSave.RequestReuse
        )

        assertThat(actualFromArguments.showCheckboxControlledFields).isTrue()
    }

    @Test
    fun `getFormArguments newLPM WITH NO customer requested save and Card`() {
        val actualFromArguments = testCardFormArguments(
            PaymentSelection.CustomerRequestedSave.NoRequest
        )

        assertThat(actualFromArguments.showCheckboxControlledFields).isFalse()
    }

    private fun testCardFormArguments(
        customerReuse: PaymentSelection.CustomerRequestedSave,
    ): FormFragmentArguments {
        val paymentIntent = mock<PaymentIntent>().also {
            whenever(it.paymentMethodTypes).thenReturn(listOf("card", "bancontact"))
        }

        val paymentMethodCreateParams = PaymentMethodCreateParams.createWithOverride(
            "card",
            false,
            mapOf(
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
            emptySet()
        )

        val actualArgs = FormArgumentsFactory.create(
            showPaymentMethod = LpmRepository.HardcodedCard,
            stripeIntent = paymentIntent,
            config = PaymentSheetFixtures.CONFIG_MINIMUM,
            merchantName = PaymentSheetFixtures.MERCHANT_DISPLAY_NAME,
            amount = Amount(50, "USD"),
            newLpm = PaymentSelection.New.Card(
                paymentMethodCreateParams = paymentMethodCreateParams,
                brand = CardBrand.Visa,
                customerRequestedSave = customerReuse
            )
        )

        assertThat(actualArgs.initialPaymentMethodCreateParams).isEqualTo(paymentMethodCreateParams)

        return actualArgs
    }
}
