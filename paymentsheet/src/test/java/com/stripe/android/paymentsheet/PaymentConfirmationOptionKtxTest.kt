package com.stripe.android.paymentsheet

import com.google.common.truth.Truth.assertThat
import com.stripe.android.core.strings.resolvableString
import com.stripe.android.model.Address
import com.stripe.android.model.CardBrand
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.PaymentMethodCreateParams
import com.stripe.android.model.PaymentMethodCreateParamsFixtures
import com.stripe.android.model.PaymentMethodFixtures
import com.stripe.android.model.PaymentMethodOptionsParams
import com.stripe.android.paymentsheet.model.PaymentSelection
import org.junit.Test

class PaymentConfirmationOptionKtxTest {
    @Test
    fun `On new selection, should convert to new confirmation option properly`() {
        val paymentSelection = createNewPaymentSelection(
            optionsParams = PaymentMethodOptionsParams.Card(
                network = "cartes_bancaires"
            ),
        )

        assertThat(
            paymentSelection.toPaymentConfirmationOption(
                initializationMode = PI_INITIALIZATION_MODE,
                configuration = PaymentSheetFixtures.CONFIG_CUSTOMER,
            )
        ).isEqualTo(
            PaymentConfirmationOption.PaymentMethod.New(
                initializationMode = PI_INITIALIZATION_MODE,
                shippingDetails = null,
                createParams = PaymentMethodCreateParamsFixtures.DEFAULT_CARD,
                optionsParams = PaymentMethodOptionsParams.Card(network = "cartes_bancaires"),
                shouldSave = false,
            )
        )
    }

    @Test
    fun `On new selection with requested no reuse, should convert to new confirmation option properly`() {
        val paymentSelection = createNewPaymentSelection(
            customerRequestedSave = PaymentSelection.CustomerRequestedSave.RequestNoReuse,
        )

        assertThat(
            paymentSelection.toPaymentConfirmationOption(
                initializationMode = SI_INITIALIZATION_MODE,
                configuration = PaymentSheetFixtures.CONFIG_CUSTOMER,
            )
        ).isEqualTo(
            PaymentConfirmationOption.PaymentMethod.New(
                initializationMode = SI_INITIALIZATION_MODE,
                shippingDetails = null,
                createParams = PaymentMethodCreateParamsFixtures.DEFAULT_CARD,
                optionsParams = null,
                shouldSave = false,
            )
        )
    }

    @Test
    fun `On new selection with requested reuse, should convert to new confirmation option properly`() {
        val paymentSelection = createNewPaymentSelection(
            customerRequestedSave = PaymentSelection.CustomerRequestedSave.RequestReuse,
        )

        assertThat(
            paymentSelection.toPaymentConfirmationOption(
                initializationMode = PI_INITIALIZATION_MODE,
                configuration = PaymentSheetFixtures.CONFIG_CUSTOMER,
            )
        ).isEqualTo(
            PaymentConfirmationOption.PaymentMethod.New(
                initializationMode = PI_INITIALIZATION_MODE,
                shippingDetails = null,
                createParams = PaymentMethodCreateParamsFixtures.DEFAULT_CARD,
                optionsParams = null,
                shouldSave = true,
            )
        )
    }

    @Test
    fun `On Bacs selection, should convert to Bacs confirmation option properly`() {
        val bacsDebitParams = PaymentMethodCreateParams.create(
            bacsDebit = PaymentMethodCreateParams.BacsDebit(
                accountNumber = "00012345",
                sortCode = "108800"
            ),
            billingDetails = PaymentMethod.BillingDetails(
                name = "John Doe",
                email = "email@email.com",
            ),
        )

        val paymentSelection = createNewPaymentSelection(
            createParams = bacsDebitParams,
        )

        assertThat(
            paymentSelection.toPaymentConfirmationOption(
                initializationMode = PI_INITIALIZATION_MODE,
                configuration = PaymentSheetFixtures.CONFIG_CUSTOMER
            )
        ).isEqualTo(
            PaymentConfirmationOption.BacsPaymentMethod(
                initializationMode = PI_INITIALIZATION_MODE,
                shippingDetails = null,
                createParams = bacsDebitParams,
                optionsParams = null,
                appearance = PaymentSheetFixtures.CONFIG_CUSTOMER.appearance,
            )
        )
    }

    @Test
    fun `On new selection with no reuse request, should convert to new confirmation option properly`() {
        val paymentSelection = createNewPaymentSelection(
            customerRequestedSave = PaymentSelection.CustomerRequestedSave.NoRequest,
        )

        assertThat(
            paymentSelection.toPaymentConfirmationOption(
                initializationMode = SI_INITIALIZATION_MODE,
                configuration = PaymentSheetFixtures.CONFIG_CUSTOMER,
            )
        ).isEqualTo(
            PaymentConfirmationOption.PaymentMethod.New(
                initializationMode = SI_INITIALIZATION_MODE,
                shippingDetails = null,
                createParams = PaymentMethodCreateParamsFixtures.DEFAULT_CARD,
                optionsParams = null,
                shouldSave = false,
            )
        )
    }

    @Test
    fun `On saved selection, should convert to saved confirmation option properly`() {
        val paymentSelection = PaymentSelection.Saved(
            paymentMethod = PaymentMethodFixtures.CARD_PAYMENT_METHOD,
            paymentMethodOptionsParams = PaymentMethodOptionsParams.Card(
                cvc = "505"
            ),
        )

        assertThat(
            paymentSelection.toPaymentConfirmationOption(
                initializationMode = PI_INITIALIZATION_MODE,
                configuration = PaymentSheetFixtures.CONFIG_CUSTOMER,
            )
        ).isEqualTo(
            PaymentConfirmationOption.PaymentMethod.Saved(
                initializationMode = PI_INITIALIZATION_MODE,
                shippingDetails = null,
                paymentMethod = PaymentMethodFixtures.CARD_PAYMENT_METHOD,
                optionsParams = PaymentMethodOptionsParams.Card(
                    cvc = "505"
                ),
            )
        )
    }

    @Test
    fun `On EPM selection, should convert to EPM confirmation option properly`() {
        val paymentSelection = PaymentSelection.ExternalPaymentMethod(
            type = "paypal",
            billingDetails = PaymentMethod.BillingDetails(
                name = "John Doe",
                address = Address(
                    city = "South San Francisco"
                )
            ),
            darkThemeIconUrl = null,
            lightThemeIconUrl = null,
            label = "Paypal".resolvableString,
            iconResource = 0,
        )

        assertThat(
            paymentSelection.toPaymentConfirmationOption(
                initializationMode = PI_INITIALIZATION_MODE,
                configuration = PaymentSheetFixtures.CONFIG_CUSTOMER,
            )
        ).isEqualTo(
            PaymentConfirmationOption.ExternalPaymentMethod(
                type = "paypal",
                billingDetails = PaymentMethod.BillingDetails(
                    name = "John Doe",
                    address = Address(
                        city = "South San Francisco"
                    )
                )
            )
        )
    }

    @Test
    fun `On Google Pay selection with config with null google pay config, should return null`() {
        assertThat(
            PaymentSelection.GooglePay.toPaymentConfirmationOption(
                initializationMode = PI_INITIALIZATION_MODE,
                configuration = PaymentSheetFixtures.CONFIG_CUSTOMER,
            )
        ).isNull()
    }

    @Test
    fun `On Google Pay selection with config with google pay config, should return expected option`() {
        assertThat(
            PaymentSelection.GooglePay.toPaymentConfirmationOption(
                initializationMode = SI_INITIALIZATION_MODE,
                configuration = PaymentSheetFixtures.CONFIG_GOOGLEPAY.copy(
                    googlePay = PaymentSheetFixtures.CONFIG_GOOGLEPAY.googlePay?.copy(
                        label = "Merchant Payments",
                        amount = 5000,
                        environment = PaymentSheet.GooglePayConfiguration.Environment.Production
                    )
                )
            )
        ).isEqualTo(
            PaymentConfirmationOption.GooglePay(
                initializationMode = SI_INITIALIZATION_MODE,
                shippingDetails = null,
                config = PaymentConfirmationOption.GooglePay.Config(
                    environment = PaymentSheet.GooglePayConfiguration.Environment.Production,
                    merchantName = "Merchant, Inc.",
                    merchantCountryCode = "US",
                    merchantCurrencyCode = "USD",
                    customAmount = 5000,
                    customLabel = "Merchant Payments",
                    billingDetailsCollectionConfiguration = PaymentSheet.BillingDetailsCollectionConfiguration()
                )
            )
        )
    }

    @Test
    fun `On Link selection, should return null`() {
        assertThat(
            PaymentSelection.Link.toPaymentConfirmationOption(
                initializationMode = PI_INITIALIZATION_MODE,
                configuration = PaymentSheetFixtures.CONFIG_CUSTOMER,
            )
        ).isNull()
    }

    private fun createNewPaymentSelection(
        createParams: PaymentMethodCreateParams = PaymentMethodCreateParamsFixtures.DEFAULT_CARD,
        optionsParams: PaymentMethodOptionsParams? = null,
        customerRequestedSave: PaymentSelection.CustomerRequestedSave =
            PaymentSelection.CustomerRequestedSave.NoRequest,
    ): PaymentSelection.New {
        return PaymentSelection.New.Card(
            paymentMethodCreateParams = createParams,
            paymentMethodOptionsParams = optionsParams,
            brand = CardBrand.CartesBancaires,
            customerRequestedSave = customerRequestedSave,
        )
    }

    private companion object {
        val PI_INITIALIZATION_MODE = PaymentSheet.InitializationMode.PaymentIntent(
            clientSecret = "pi_123"
        )

        val SI_INITIALIZATION_MODE = PaymentSheet.InitializationMode.SetupIntent(
            clientSecret = "pi_123"
        )
    }
}
