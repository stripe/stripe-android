package com.stripe.android.paymentelement.confirmation

import com.google.common.truth.Truth.assertThat
import com.stripe.android.common.model.asCommonConfiguration
import com.stripe.android.core.strings.resolvableString
import com.stripe.android.link.LinkConfiguration
import com.stripe.android.lpmfoundations.paymentmethod.PaymentSheetCardBrandFilter
import com.stripe.android.model.Address
import com.stripe.android.model.CardBrand
import com.stripe.android.model.LinkMode
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.PaymentMethodCreateParams
import com.stripe.android.model.PaymentMethodCreateParamsFixtures
import com.stripe.android.model.PaymentMethodFixtures
import com.stripe.android.model.PaymentMethodOptionsParams
import com.stripe.android.paymentelement.confirmation.bacs.BacsConfirmationOption
import com.stripe.android.paymentelement.confirmation.epms.ExternalPaymentMethodConfirmationOption
import com.stripe.android.paymentelement.confirmation.gpay.GooglePayConfirmationOption
import com.stripe.android.paymentelement.confirmation.link.LinkConfirmationOption
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.PaymentSheetFixtures
import com.stripe.android.paymentsheet.R
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.state.PaymentElementLoader
import com.stripe.android.testing.PaymentIntentFactory
import com.stripe.android.testing.PaymentMethodFactory
import com.stripe.android.utils.BankFormScreenStateFactory
import org.junit.Test

class ConfirmationHandlerOptionKtxTest {
    @Test
    fun `On new selection, should convert to new confirmation option properly`() {
        val paymentSelection = createNewPaymentSelection(
            optionsParams = PaymentMethodOptionsParams.Card(
                network = "cartes_bancaires"
            ),
        )

        assertThat(
            paymentSelection.toConfirmationOption(
                configuration = PaymentSheetFixtures.CONFIG_CUSTOMER.asCommonConfiguration(),
                linkConfiguration = null,
            )
        ).isEqualTo(
            PaymentMethodConfirmationOption.New(
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
            paymentSelection.toConfirmationOption(
                configuration = PaymentSheetFixtures.CONFIG_CUSTOMER.asCommonConfiguration(),
                linkConfiguration = null,
            )
        ).isEqualTo(
            PaymentMethodConfirmationOption.New(
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
            paymentSelection.toConfirmationOption(
                configuration = PaymentSheetFixtures.CONFIG_CUSTOMER.asCommonConfiguration(),
                linkConfiguration = null,
            )
        ).isEqualTo(
            PaymentMethodConfirmationOption.New(
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
            paymentSelection.toConfirmationOption(
                configuration = PaymentSheetFixtures.CONFIG_CUSTOMER.asCommonConfiguration(),
                linkConfiguration = null,
            )
        ).isEqualTo(
            BacsConfirmationOption(
                createParams = bacsDebitParams,
                optionsParams = null,
            )
        )
    }

    @Test
    fun `On new selection with no reuse request, should convert to new confirmation option properly`() {
        val paymentSelection = createNewPaymentSelection(
            customerRequestedSave = PaymentSelection.CustomerRequestedSave.NoRequest,
        )

        assertThat(
            paymentSelection.toConfirmationOption(
                configuration = PaymentSheetFixtures.CONFIG_CUSTOMER.asCommonConfiguration(),
                linkConfiguration = null,
            )
        ).isEqualTo(
            PaymentMethodConfirmationOption.New(
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
            paymentSelection.toConfirmationOption(
                configuration = PaymentSheetFixtures.CONFIG_CUSTOMER.asCommonConfiguration(),
                linkConfiguration = null,
            )
        ).isEqualTo(
            PaymentMethodConfirmationOption.Saved(
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
            paymentSelection.toConfirmationOption(
                configuration = PaymentSheetFixtures.CONFIG_CUSTOMER.asCommonConfiguration(),
                linkConfiguration = null,
            )
        ).isEqualTo(
            ExternalPaymentMethodConfirmationOption(
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
            PaymentSelection.GooglePay.toConfirmationOption(
                configuration = PaymentSheetFixtures.CONFIG_CUSTOMER.asCommonConfiguration(),
                linkConfiguration = null,
            )
        ).isNull()
    }

    @Test
    fun `On Google Pay selection with config with google pay config, should return expected option`() {
        assertThat(
            PaymentSelection.GooglePay.toConfirmationOption(
                configuration = PaymentSheetFixtures.CONFIG_GOOGLEPAY.copy(
                    googlePay = PaymentSheetFixtures.CONFIG_GOOGLEPAY.googlePay?.copy(
                        label = "Merchant Payments",
                        amount = 5000,
                        environment = PaymentSheet.GooglePayConfiguration.Environment.Production
                    )
                ).asCommonConfiguration(),
                linkConfiguration = null,
            )
        ).isEqualTo(
            GooglePayConfirmationOption(
                config = GooglePayConfirmationOption.Config(
                    environment = PaymentSheet.GooglePayConfiguration.Environment.Production,
                    merchantName = "Merchant, Inc.",
                    merchantCountryCode = "US",
                    merchantCurrencyCode = "USD",
                    customAmount = 5000,
                    customLabel = "Merchant Payments",
                    billingDetailsCollectionConfiguration = PaymentSheet.BillingDetailsCollectionConfiguration(),
                    cardBrandFilter = PaymentSheetCardBrandFilter(
                        cardBrandAcceptance = PaymentSheet.CardBrandAcceptance.All
                    )
                )
            )
        )
    }

    @Test
    fun `On Link selection but with no configuration, should return null`() {
        assertThat(
            PaymentSelection.Link.toConfirmationOption(
                configuration = PaymentSheetFixtures.CONFIG_CUSTOMER.asCommonConfiguration(),
                linkConfiguration = null,
            )
        ).isNull()
    }

    @Test
    fun `On Link selection with configuration, should return Link confirmation option`() {
        assertThat(
            PaymentSelection.Link.toConfirmationOption(
                configuration = PaymentSheetFixtures.CONFIG_CUSTOMER.asCommonConfiguration(),
                linkConfiguration = LINK_CONFIGURATION,
            )
        ).isEqualTo(
            LinkConfirmationOption(
                configuration = LINK_CONFIGURATION,
            )
        )
    }

    @Test
    fun `Converts Instant Debits into a saved payment confirmation option`() {
        val paymentSelection = createNewBankAccountPaymentSelection(linkMode = LinkMode.LinkPaymentMethod)

        val expectedPaymentMethod = PaymentMethodFactory.instantDebits()

        assertThat(
            paymentSelection.toConfirmationOption(
                configuration = PaymentSheetFixtures.CONFIG_CUSTOMER.asCommonConfiguration(),
                linkConfiguration = null,
            )
        ).isEqualTo(
            PaymentMethodConfirmationOption.Saved(
                optionsParams = null,
                paymentMethod = expectedPaymentMethod,
            )
        )
    }

    @Test
    fun `Converts Link Card Brand into a saved payment confirmation option`() {
        val paymentSelection = createNewBankAccountPaymentSelection(linkMode = LinkMode.LinkCardBrand)

        val expectedPaymentMethod = PaymentMethodFactory.instantDebits()

        assertThat(
            paymentSelection.toConfirmationOption(
                configuration = PaymentSheetFixtures.CONFIG_CUSTOMER.asCommonConfiguration(),
                linkConfiguration = null,
            )
        ).isEqualTo(
            PaymentMethodConfirmationOption.Saved(
                optionsParams = null,
                paymentMethod = expectedPaymentMethod,
            )
        )
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

    private fun createNewBankAccountPaymentSelection(
        linkMode: LinkMode?,
    ): PaymentSelection.New.USBankAccount {
        return PaymentSelection.New.USBankAccount(
            label = "•••• 4242",
            iconResource = R.drawable.stripe_ic_paymentsheet_bank,
            paymentMethodCreateParams = PaymentMethodCreateParamsFixtures.US_BANK_ACCOUNT,
            paymentMethodOptionsParams = null,
            customerRequestedSave = PaymentSelection.CustomerRequestedSave.NoRequest,
            screenState = BankFormScreenStateFactory.createWithPaymentMethod(
                paymentMethod = PaymentMethodFactory.instantDebits(),
            ),
            instantDebits = PaymentSelection.New.USBankAccount.InstantDebitsInfo(
                paymentMethod = PaymentMethodFactory.instantDebits(),
                linkMode = linkMode,
            ),
            input = PaymentSelection.New.USBankAccount.Input(
                name = "",
                email = "email@email.com",
                phone = null,
                address = null,
                saveForFutureUse = false,
            ),
        )
    }

    private companion object {
        val PI_INITIALIZATION_MODE = PaymentElementLoader.InitializationMode.PaymentIntent(
            clientSecret = "pi_123"
        )

        val SI_INITIALIZATION_MODE = PaymentElementLoader.InitializationMode.SetupIntent(
            clientSecret = "pi_123"
        )

        val LINK_CONFIGURATION = LinkConfiguration(
            stripeIntent = PaymentIntentFactory.create(),
            merchantName = "Merchant, Inc.",
            merchantCountryCode = "CA",
            customerInfo = LinkConfiguration.CustomerInfo(
                name = "John Doe",
                email = null,
                phone = null,
                billingCountryCode = "CA",
            ),
            shippingDetails = null,
            passthroughModeEnabled = false,
            cardBrandChoice = null,
            flags = mapOf(),
            useAttestationEndpointsForLink = true,
        )
    }
}
